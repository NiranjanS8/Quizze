package com.quizze.quizze.quiz.service;

import com.quizze.quizze.common.exception.BadRequestException;
import com.quizze.quizze.common.exception.ResourceNotFoundException;
import com.quizze.quizze.quiz.domain.AttemptAnswer;
import com.quizze.quizze.quiz.domain.AttemptStatus;
import com.quizze.quizze.quiz.domain.DifficultyLevel;
import com.quizze.quizze.quiz.domain.Option;
import com.quizze.quizze.quiz.domain.Question;
import com.quizze.quizze.quiz.domain.Quiz;
import com.quizze.quizze.quiz.domain.QuizAttempt;
import com.quizze.quizze.quiz.dto.user.AttemptAnswerResultResponse;
import com.quizze.quizze.quiz.dto.user.AttemptHistoryResponse;
import com.quizze.quizze.quiz.dto.user.AttemptOptionResponse;
import com.quizze.quizze.quiz.dto.user.AttemptQuestionResponse;
import com.quizze.quizze.quiz.dto.user.AttemptQuestionsResponse;
import com.quizze.quizze.quiz.dto.user.QuizCatalogResponse;
import com.quizze.quizze.quiz.dto.user.QuizDetailResponse;
import com.quizze.quizze.quiz.dto.user.QuizResultResponse;
import com.quizze.quizze.quiz.dto.user.QuizSummaryResponse;
import com.quizze.quizze.quiz.dto.user.StartQuizResponse;
import com.quizze.quizze.quiz.dto.user.SubmitAnswerRequest;
import com.quizze.quizze.quiz.dto.user.SubmitQuizRequest;
import com.quizze.quizze.quiz.dto.user.SubmitQuizResponse;
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
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserQuizService {

    private static final double NEGATIVE_MARKING_FACTOR = 0.25;

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final UserRepository userRepository;

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

        return QuizCatalogResponse.builder()
                .content(quizPage.getContent().stream().map(this::mapQuizSummary).toList())
                .availableCategories(quizRepository.findPublishedCategoryNames())
                .pageNumber(quizPage.getNumber())
                .pageSize(quizPage.getSize())
                .totalPages(quizPage.getTotalPages())
                .totalElements(quizPage.getTotalElements())
                .hasNext(quizPage.hasNext())
                .hasPrevious(quizPage.hasPrevious())
                .build();
    }

    @Transactional(readOnly = true)
    public QuizDetailResponse getPublishedQuizDetails(Long quizId) {
        Quiz quiz = getPublishedQuiz(quizId);
        return mapQuizDetail(quiz);
    }

    @Transactional
    public StartQuizResponse startQuiz(Long quizId, Long userId) {
        Quiz quiz = getPublishedQuiz(quizId);
        User user = getUser(userId);

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

        return StartQuizResponse.builder()
                .attemptId(savedAttempt.getId())
                .quizId(quiz.getId())
                .quizTitle(quiz.getTitle())
                .status(savedAttempt.getStatus())
                .startedAt(savedAttempt.getStartedAt())
                .expiresAt(expiresAt)
                .timeLimitInMinutes(quiz.getTimeLimitInMinutes())
                .questionCount(quiz.getQuestions().size())
                .build();
    }

    @Transactional(readOnly = true)
    public AttemptQuestionsResponse getAttemptQuestions(Long attemptId, Long userId) {
        QuizAttempt attempt = getUserAttempt(attemptId, userId);

        if (attempt.getStatus() == AttemptStatus.NOT_STARTED) {
            throw new BadRequestException("Quiz attempt has not been started");
        }

        LocalDateTime expiresAt = calculateExpiresAt(attempt);
        boolean timeExpired = isTimedOut(attempt);

        List<AttemptQuestionResponse> questions = getOrderedQuestions(attempt).stream()
                .map(question -> AttemptQuestionResponse.builder()
                        .id(question.getId())
                        .content(question.getContent())
                        .points(question.getPoints())
                        .options(question.getOptions().stream()
                                .sorted(Comparator.comparing(Option::getId))
                                .map(option -> AttemptOptionResponse.builder()
                                        .id(option.getId())
                                        .content(option.getContent())
                                        .build())
                                .toList())
                        .build())
                .toList();

        return AttemptQuestionsResponse.builder()
                .attemptId(attempt.getId())
                .quizId(attempt.getQuiz().getId())
                .quizTitle(attempt.getQuiz().getTitle())
                .startedAt(attempt.getStartedAt())
                .expiresAt(expiresAt)
                .timeLimitInMinutes(attempt.getQuiz().getTimeLimitInMinutes())
                .timeExpired(timeExpired)
                .questions(questions)
                .build();
    }

    @Transactional
    public SubmitQuizResponse submitQuiz(Long attemptId, Long userId, SubmitQuizRequest request) {
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

        return SubmitQuizResponse.builder()
                .attemptId(attempt.getId())
                .status(attempt.getStatus())
                .submittedAt(attempt.getSubmittedAt())
                .score(attempt.getScore())
                .maxScore(maxScore)
                .percentage(calculatePercentage(attempt.getScore(), maxScore))
                .correctAnswers(attempt.getCorrectAnswers())
                .wrongAnswers(attempt.getWrongAnswers())
                .timeExpired(timeExpired)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AttemptHistoryResponse> getAttemptHistory(Long userId) {
        return quizAttemptRepository.findByUserId(userId).stream()
                .sorted(Comparator.comparing(QuizAttempt::getCreatedAt).reversed())
                .map(this::mapAttemptHistory)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuizResultResponse> getResultHistory(Long userId) {
        return quizAttemptRepository.findByUserId(userId).stream()
                .filter(attempt -> attempt.getStatus() == AttemptStatus.SUBMITTED)
                .sorted(Comparator.comparing(QuizAttempt::getSubmittedAt).reversed())
                .map(this::mapQuizResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuizResultResponse getAttemptResult(Long attemptId, Long userId) {
        QuizAttempt attempt = getUserAttempt(attemptId, userId);
        if (attempt.getStatus() != AttemptStatus.SUBMITTED) {
            throw new BadRequestException("Result is available only after the quiz is submitted");
        }
        return mapQuizResult(attempt);
    }

    private void validateSubmittedAnswers(QuizAttempt attempt, SubmitQuizRequest request) {
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

    private QuizSummaryResponse mapQuizSummary(Quiz quiz) {
        return QuizSummaryResponse.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .categoryName(quiz.getCategory() == null ? null : quiz.getCategory().getName())
                .difficulty(quiz.getDifficulty())
                .timeLimitInMinutes(quiz.getTimeLimitInMinutes())
                .questionCount(quiz.getQuestions().size())
                .oneAttemptOnly(quiz.isOneAttemptOnly())
                .negativeMarkingEnabled(quiz.isNegativeMarkingEnabled())
                .build();
    }

    private AttemptHistoryResponse mapAttemptHistory(QuizAttempt attempt) {
        double maxScore = calculateMaxScore(attempt.getQuiz());
        return AttemptHistoryResponse.builder()
                .attemptId(attempt.getId())
                .quizId(attempt.getQuiz().getId())
                .quizTitle(attempt.getQuiz().getTitle())
                .status(attempt.getStatus())
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .score(attempt.getScore())
                .maxScore(maxScore)
                .percentage(calculatePercentage(attempt.getScore(), maxScore))
                .correctAnswers(attempt.getCorrectAnswers())
                .wrongAnswers(attempt.getWrongAnswers())
                .build();
    }

    private QuizDetailResponse mapQuizDetail(Quiz quiz) {
        return QuizDetailResponse.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .categoryName(quiz.getCategory() == null ? null : quiz.getCategory().getName())
                .difficulty(quiz.getDifficulty())
                .timeLimitInMinutes(quiz.getTimeLimitInMinutes())
                .oneAttemptOnly(quiz.isOneAttemptOnly())
                .negativeMarkingEnabled(quiz.isNegativeMarkingEnabled())
                .questionCount(quiz.getQuestions().size())
                .build();
    }

    private QuizResultResponse mapQuizResult(QuizAttempt attempt) {
        double maxScore = calculateMaxScore(attempt.getQuiz());

        List<AttemptAnswerResultResponse> answers = attempt.getAnswers().stream()
                .sorted(Comparator.comparing(answer -> answer.getQuestion().getId()))
                .map(answer -> AttemptAnswerResultResponse.builder()
                        .questionId(answer.getQuestion().getId())
                        .questionContent(answer.getQuestion().getContent())
                        .selectedOptionId(answer.getSelectedOption() == null ? null : answer.getSelectedOption().getId())
                        .selectedOptionContent(answer.getSelectedOption() == null ? null : answer.getSelectedOption().getContent())
                        .correct(Boolean.TRUE.equals(answer.getCorrect()))
                        .points(answer.getQuestion().getPoints())
                        .build())
                .toList();

        return QuizResultResponse.builder()
                .attemptId(attempt.getId())
                .quizId(attempt.getQuiz().getId())
                .quizTitle(attempt.getQuiz().getTitle())
                .status(attempt.getStatus())
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .totalQuestions(attempt.getQuiz().getQuestions().size())
                .attemptedQuestions(attempt.getAnswers().size())
                .correctAnswers(attempt.getCorrectAnswers())
                .wrongAnswers(attempt.getWrongAnswers())
                .score(attempt.getScore())
                .maxScore(maxScore)
                .percentage(calculatePercentage(attempt.getScore(), maxScore))
                .answers(answers)
                .build();
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
