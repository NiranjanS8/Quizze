package com.quizze.quizze.quiz.service;

import static com.quizze.quizze.cache.config.CacheConfig.USER_PERFORMANCE_CACHE;

import com.quizze.quizze.common.exception.BadRequestException;
import com.quizze.quizze.common.exception.ResourceNotFoundException;
import com.quizze.quizze.monitoring.service.ApplicationMetricsService;
import com.quizze.quizze.quiz.domain.AttemptAnswer;
import com.quizze.quizze.quiz.domain.AttemptStatus;
import com.quizze.quizze.quiz.domain.DifficultyLevel;
import com.quizze.quizze.quiz.domain.Option;
import com.quizze.quizze.quiz.domain.Question;
import com.quizze.quizze.quiz.domain.Quiz;
import com.quizze.quizze.quiz.domain.QuizAttempt;
import com.quizze.quizze.quiz.dto.user.AttemptHistoryResponse;
import com.quizze.quizze.quiz.dto.user.AttemptQuestionsResponse;
import com.quizze.quizze.quiz.dto.user.QuizCatalogResponse;
import com.quizze.quizze.quiz.dto.user.QuizDetailResponse;
import com.quizze.quizze.quiz.dto.user.QuizResultResponse;
import com.quizze.quizze.quiz.dto.user.StartQuizResponse;
import com.quizze.quizze.quiz.dto.user.SubmitAnswerRequest;
import com.quizze.quizze.quiz.dto.user.SubmitQuizRequest;
import com.quizze.quizze.quiz.dto.user.SubmitQuizResponse;
import com.quizze.quizze.quiz.event.QuizSubmittedEvent;
import com.quizze.quizze.quiz.dto.user.UserCategoryPerformanceResponse;
import com.quizze.quizze.quiz.dto.user.UserPerformanceAnalyticsResponse;
import com.quizze.quizze.quiz.dto.user.UserPerformanceTrendItemResponse;
import com.quizze.quizze.quiz.mapper.UserQuizMapper;
import com.quizze.quizze.quiz.repository.QuizAttemptRepository;
import com.quizze.quizze.quiz.repository.QuizRepository;
import com.quizze.quizze.user.domain.User;
import com.quizze.quizze.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserQuizService {

    private static final double NEGATIVE_MARKING_FACTOR = 0.25;

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final UserRepository userRepository;
    private final UserQuizMapper userQuizMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ApplicationMetricsService applicationMetricsService;

    @Transactional(readOnly = true)
    public QuizCatalogResponse getPublishedQuizzes(
            String search,
            String category,
            DifficultyLevel difficulty,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        log.debug(
                "Fetching published quizzes with search='{}', category='{}', difficulty='{}', page={}, size={}, sortBy='{}', sortDir='{}'",
                search, category, difficulty, page, size, sortBy, sortDir
        );
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50),
                Sort.by(resolveSortDirection(sortDir), resolveSortProperty(sortBy))
        );

        Specification<Quiz> specification = Specification.where(isPublished())
                .and(matchesSearch(search))
                .and(matchesCategory(category))
                .and(matchesDifficulty(difficulty));

        Page<Quiz> quizPage = quizRepository.findAll(specification, pageable);
        log.debug("Published quiz query returned {} items on page {}", quizPage.getNumberOfElements(), quizPage.getNumber());

        return userQuizMapper.toQuizCatalogResponse(quizPage, quizRepository.findPublishedCategoryNames());
    }

    @Transactional(readOnly = true)
    public QuizDetailResponse getPublishedQuizDetails(Long quizId) {
        log.debug("Fetching published quiz details for quizId={}", quizId);
        Quiz quiz = getPublishedQuiz(quizId);
        return userQuizMapper.toQuizDetailResponse(quiz);
    }

    @Transactional
    public StartQuizResponse startQuiz(Long quizId, Long userId) {
        log.info("Starting quiz attempt for userId={} and quizId={}", userId, quizId);
        Quiz quiz = getPublishedQuiz(quizId);
        User user = getUser(userId);

        if (quiz.getQuestions().isEmpty()) {
            throw new BadRequestException("This quiz is not ready yet because it has no questions");
        }

        if (quiz.isOneAttemptOnly() && !quizAttemptRepository.findByUserIdAndQuizId(userId, quizId).isEmpty()) {
            throw new BadRequestException("This quiz can only be attempted once");
        }

        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuiz(quiz);
        attempt.setUser(user);
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attempt.setStartedAt(LocalDateTime.now());
        attempt.setQuestionOrder(buildRandomQuestionOrder(quiz));

        QuizAttempt savedAttempt = quizAttemptRepository.save(attempt);
        LocalDateTime expiresAt = calculateExpiresAt(savedAttempt);
        log.info("Quiz attempt started with attemptId={} for userId={} and quizId={}", savedAttempt.getId(), userId, quizId);
        applicationMetricsService.increment("quizze.quiz.attempt.started");

        return userQuizMapper.toStartQuizResponse(savedAttempt, expiresAt);
    }

    @Transactional(readOnly = true)
    public AttemptQuestionsResponse getAttemptQuestions(Long attemptId, Long userId) {
        log.debug("Fetching attempt questions for attemptId={} and userId={}", attemptId, userId);
        QuizAttempt attempt = getUserAttempt(attemptId, userId);

        if (attempt.getStatus() == AttemptStatus.NOT_STARTED) {
            throw new BadRequestException("Quiz attempt has not been started");
        }

        LocalDateTime expiresAt = calculateExpiresAt(attempt);
        boolean timeExpired = isTimedOut(attempt);

        return userQuizMapper.toAttemptQuestionsResponse(
                attempt,
                expiresAt,
                timeExpired,
                getOrderedQuestions(attempt)
        );
    }

    @Transactional
    public SubmitQuizResponse submitQuiz(Long attemptId, Long userId, SubmitQuizRequest request) {
        log.info("Submitting quiz attemptId={} for userId={} with {} answers", attemptId, userId, request.getAnswers().size());
        QuizAttempt attempt = getUserAttempt(attemptId, userId);

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new BadRequestException("Only in-progress attempts can be submitted");
        }

        boolean timeExpired = isTimedOut(attempt);
        validateSubmittedAnswers(attempt, request);

        attempt.getAnswers().clear();

        double rawScore = 0.0;
        int correctAnswers = 0;
        int wrongAnswers = 0;

        Map<Long, SubmitAnswerRequest> submittedAnswersByQuestionId = new HashMap<>();
        for (SubmitAnswerRequest submittedAnswer : request.getAnswers()) {
            submittedAnswersByQuestionId.put(submittedAnswer.getQuestionId(), submittedAnswer);
        }

        for (Question question : getOrderedQuestions(attempt)) {
            SubmitAnswerRequest submittedAnswer = submittedAnswersByQuestionId.get(question.getId());
            if (submittedAnswer == null) {
                continue;
            }

            Option selectedOption = question.getOptions().stream()
                    .filter(option -> option.getId().equals(submittedAnswer.getSelectedOptionId()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("Selected option does not belong to the given question"));

            boolean correct = selectedOption.isCorrect();
            if (correct) {
                correctAnswers++;
                rawScore += question.getPoints();
            } else {
                wrongAnswers++;
                if (attempt.getQuiz().isNegativeMarkingEnabled()) {
                    rawScore -= question.getPoints() * NEGATIVE_MARKING_FACTOR;
                }
            }

            AttemptAnswer answer = new AttemptAnswer();
            answer.setQuizAttempt(attempt);
            answer.setQuestion(question);
            answer.setSelectedOption(selectedOption);
            answer.setCorrect(correct);
            attempt.getAnswers().add(answer);
        }

        double maxScore = calculateMaxScore(attempt.getQuiz());
        double finalScore = Math.max(0.0, rawScore);

        attempt.setScore(finalScore);
        attempt.setCorrectAnswers(correctAnswers);
        attempt.setWrongAnswers(wrongAnswers);
        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(LocalDateTime.now());
        log.info(
                "Quiz submitted successfully for attemptId={} with score={}, correctAnswers={}, wrongAnswers={}, timeExpired={}",
                attempt.getId(), attempt.getScore(), correctAnswers, wrongAnswers, timeExpired
        );
        applicationMetricsService.increment("quizze.quiz.attempt.submitted");
        applicationEventPublisher.publishEvent(new QuizSubmittedEvent(attempt.getQuiz().getId(), userId, attempt.getId()));

        return userQuizMapper.toSubmitQuizResponse(
                attempt,
                maxScore,
                calculatePercentage(attempt.getScore(), maxScore),
                timeExpired
        );
    }

    @Transactional(readOnly = true)
    public List<AttemptHistoryResponse> getAttemptHistory(Long userId) {
        log.debug("Fetching attempt history for userId={}", userId);
        return quizAttemptRepository.findByUserId(userId).stream()
                .sorted(Comparator.comparing(QuizAttempt::getCreatedAt).reversed())
                .map(this::toAttemptHistoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuizResultResponse> getResultHistory(Long userId) {
        log.debug("Fetching result history for userId={}", userId);
        return quizAttemptRepository.findByUserId(userId).stream()
                .filter(attempt -> attempt.getStatus() == AttemptStatus.SUBMITTED)
                .sorted(Comparator.comparing(QuizAttempt::getSubmittedAt).reversed())
                .map(this::toQuizResultResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuizResultResponse getAttemptResult(Long attemptId, Long userId) {
        log.debug("Fetching attempt result for attemptId={} and userId={}", attemptId, userId);
        QuizAttempt attempt = getUserAttempt(attemptId, userId);
        if (attempt.getStatus() != AttemptStatus.SUBMITTED) {
            throw new BadRequestException("Result is available only after the quiz is submitted");
        }
        return toQuizResultResponse(attempt);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = USER_PERFORMANCE_CACHE, key = "#userId")
    public UserPerformanceAnalyticsResponse getUserPerformanceAnalytics(Long userId) {
        log.debug("Fetching user performance analytics for userId={}", userId);
        List<QuizAttempt> submittedAttempts = quizAttemptRepository.findByUserId(userId).stream()
                .filter(attempt -> attempt.getStatus() == AttemptStatus.SUBMITTED)
                .sorted(Comparator.comparing(QuizAttempt::getSubmittedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        double averageScore = submittedAttempts.stream()
                .mapToDouble(QuizAttempt::getScore)
                .average()
                .orElse(0.0);

        double averagePercentage = submittedAttempts.stream()
                .mapToDouble(this::calculateAttemptPercentage)
                .average()
                .orElse(0.0);

        double bestPercentage = submittedAttempts.stream()
                .mapToDouble(this::calculateAttemptPercentage)
                .max()
                .orElse(0.0);

        Map<String, List<QuizAttempt>> attemptsByCategory = submittedAttempts.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        attempt -> attempt.getQuiz().getCategory() == null ? "Uncategorized" : attempt.getQuiz().getCategory().getName(),
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ));

        List<UserCategoryPerformanceResponse> categoryPerformances = attemptsByCategory.entrySet().stream()
                .map(entry -> toCategoryPerformanceResponse(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(UserCategoryPerformanceResponse::getAveragePercentage, Comparator.reverseOrder()))
                .toList();

        List<UserPerformanceTrendItemResponse> recentTrend = submittedAttempts.stream()
                .limit(5)
                .map(this::toTrendItemResponse)
                .toList();

        return userQuizMapper.toUserPerformanceAnalyticsResponse(
                submittedAttempts.size(),
                submittedAttempts.stream().map(attempt -> attempt.getQuiz().getId()).distinct().count(),
                averageScore,
                averagePercentage,
                bestPercentage,
                categoryPerformances.isEmpty() ? null : categoryPerformances.get(0),
                categoryPerformances.isEmpty() ? null : categoryPerformances.get(categoryPerformances.size() - 1),
                recentTrend
        );
    }

    private void validateSubmittedAnswers(QuizAttempt attempt, SubmitQuizRequest request) {
        if (request.getAnswers().size() > attempt.getQuiz().getQuestions().size()) {
            throw new BadRequestException("Submitted answers exceed the number of quiz questions");
        }

        Set<Long> allowedQuestionIds = attempt.getQuiz().getQuestions().stream()
                .map(Question::getId)
                .collect(java.util.stream.Collectors.toSet());

        Set<Long> seenQuestionIds = new HashSet<>();
        for (SubmitAnswerRequest answer : request.getAnswers()) {
            if (!allowedQuestionIds.contains(answer.getQuestionId())) {
                throw new BadRequestException("Question does not belong to this quiz");
            }

            if (!seenQuestionIds.add(answer.getQuestionId())) {
                throw new BadRequestException("Duplicate answers for the same question are not allowed");
            }
        }
    }

    private Quiz getPublishedQuiz(Long quizId) {
        return quizRepository.findByIdAndPublishedTrue(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Published quiz not found with id: " + quizId));
    }

    private QuizAttempt getUserAttempt(Long attemptId, Long userId) {
        return quizAttemptRepository.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz attempt not found with id: " + attemptId));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private AttemptHistoryResponse toAttemptHistoryResponse(QuizAttempt attempt) {
        double maxScore = calculateMaxScore(attempt.getQuiz());
        return userQuizMapper.toAttemptHistoryResponse(
                attempt,
                maxScore,
                calculatePercentage(attempt.getScore(), maxScore)
        );
    }

    private QuizResultResponse toQuizResultResponse(QuizAttempt attempt) {
        double maxScore = calculateMaxScore(attempt.getQuiz());
        return userQuizMapper.toQuizResultResponse(
                attempt,
                maxScore,
                calculatePercentage(attempt.getScore(), maxScore)
        );
    }

    private String buildRandomQuestionOrder(Quiz quiz) {
        List<Long> questionIds = quiz.getQuestions().stream()
                .map(Question::getId)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        Collections.shuffle(questionIds);
        return questionIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
    }

    private List<Question> getOrderedQuestions(QuizAttempt attempt) {
        List<Question> quizQuestions = attempt.getQuiz().getQuestions();
        if (attempt.getQuestionOrder() == null || attempt.getQuestionOrder().isBlank()) {
            return quizQuestions.stream()
                    .sorted(Comparator.comparing(Question::getId))
                    .toList();
        }

        Map<Long, Question> questionById = quizQuestions.stream()
                .collect(java.util.stream.Collectors.toMap(Question::getId, question -> question));

        List<Question> orderedQuestions = new ArrayList<>();
        for (String rawId : attempt.getQuestionOrder().split(",")) {
            if (rawId.isBlank()) {
                continue;
            }
            Question question = questionById.get(Long.parseLong(rawId.trim()));
            if (question != null) {
                orderedQuestions.add(question);
            }
        }

        if (orderedQuestions.size() == quizQuestions.size()) {
            return orderedQuestions;
        }

        Set<Long> orderedIds = orderedQuestions.stream().map(Question::getId).collect(java.util.stream.Collectors.toSet());
        quizQuestions.stream()
                .filter(Predicate.not(question -> orderedIds.contains(question.getId())))
                .sorted(Comparator.comparing(Question::getId))
                .forEach(orderedQuestions::add);

        return orderedQuestions;
    }

    private LocalDateTime calculateExpiresAt(QuizAttempt attempt) {
        Integer timeLimit = attempt.getQuiz().getTimeLimitInMinutes();
        if (timeLimit == null || timeLimit <= 0 || attempt.getStartedAt() == null) {
            return null;
        }
        return attempt.getStartedAt().plusMinutes(timeLimit);
    }

    private boolean isTimedOut(QuizAttempt attempt) {
        LocalDateTime expiresAt = calculateExpiresAt(attempt);
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    private double calculateMaxScore(Quiz quiz) {
        return quiz.getQuestions().stream().mapToInt(Question::getPoints).sum();
    }

    private double calculatePercentage(double score, double maxScore) {
        if (maxScore == 0.0) {
            return 0.0;
        }
        return Math.max(0.0, (score / maxScore) * 100.0);
    }

    private double calculateAttemptPercentage(QuizAttempt attempt) {
        return calculatePercentage(attempt.getScore(), calculateMaxScore(attempt.getQuiz()));
    }

    private UserCategoryPerformanceResponse toCategoryPerformanceResponse(String categoryName, List<QuizAttempt> attempts) {
        return userQuizMapper.toUserCategoryPerformanceResponse(
                categoryName,
                attempts.size(),
                attempts.stream().mapToDouble(QuizAttempt::getScore).average().orElse(0.0),
                attempts.stream().mapToDouble(this::calculateAttemptPercentage).average().orElse(0.0)
        );
    }

    private UserPerformanceTrendItemResponse toTrendItemResponse(QuizAttempt attempt) {
        String categoryName = attempt.getQuiz().getCategory() == null
                ? "Uncategorized"
                : attempt.getQuiz().getCategory().getName();

        return userQuizMapper.toUserPerformanceTrendItemResponse(
                attempt,
                categoryName,
                calculateMaxScore(attempt.getQuiz()),
                calculateAttemptPercentage(attempt)
        );
    }

    private Sort.Direction resolveSortDirection(String sortDir) {
        return "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
    }

    private String resolveSortProperty(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "createdAt";
        }

        return switch (sortBy) {
            case "title" -> "title";
            case "difficulty" -> "difficulty";
            case "timeLimitInMinutes" -> "timeLimitInMinutes";
            case "createdAt" -> "createdAt";
            default -> "createdAt";
        };
    }

    private Specification<Quiz> isPublished() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isTrue(root.get("published"));
    }

    private Specification<Quiz> matchesSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }

        String normalizedSearch = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), normalizedSearch),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), normalizedSearch),
                criteriaBuilder.like(criteriaBuilder.lower(root.join("category", jakarta.persistence.criteria.JoinType.LEFT).get("name")), normalizedSearch)
        );
    }

    private Specification<Quiz> matchesCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }

        String normalizedCategory = category.trim().toLowerCase(Locale.ROOT);
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
                criteriaBuilder.lower(root.join("category", jakarta.persistence.criteria.JoinType.LEFT).get("name")),
                normalizedCategory
        );
    }

    private Specification<Quiz> matchesDifficulty(DifficultyLevel difficulty) {
        if (difficulty == null) {
            return null;
        }

        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("difficulty"), difficulty);
    }
}
