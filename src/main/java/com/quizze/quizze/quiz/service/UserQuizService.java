package com.quizze.quizze.quiz.service;

import com.quizze.quizze.common.exception.BadRequestException;
import com.quizze.quizze.common.exception.ResourceNotFoundException;
import com.quizze.quizze.quiz.domain.AttemptAnswer;
import com.quizze.quizze.quiz.domain.AttemptStatus;
import com.quizze.quizze.quiz.domain.Option;
import com.quizze.quizze.quiz.domain.Question;
import com.quizze.quizze.quiz.domain.Quiz;
import com.quizze.quizze.quiz.domain.QuizAttempt;
import com.quizze.quizze.quiz.dto.user.AttemptHistoryResponse;
import com.quizze.quizze.quiz.dto.user.AttemptAnswerResultResponse;
import com.quizze.quizze.quiz.dto.user.AttemptOptionResponse;
import com.quizze.quizze.quiz.dto.user.AttemptQuestionResponse;
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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserQuizService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<QuizSummaryResponse> getPublishedQuizzes() {
        return quizRepository.findByPublishedTrue()
                .stream()
                .sorted(Comparator.comparing(Quiz::getCreatedAt).reversed())
                .map(this::mapQuizSummary)
                .toList();
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

        QuizAttempt savedAttempt = quizAttemptRepository.save(attempt);

        return StartQuizResponse.builder()
                .attemptId(savedAttempt.getId())
                .quizId(quiz.getId())
                .quizTitle(quiz.getTitle())
                .status(savedAttempt.getStatus())
                .startedAt(savedAttempt.getStartedAt())
                .questionCount(quiz.getQuestions().size())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AttemptQuestionResponse> getAttemptQuestions(Long attemptId, Long userId) {
        QuizAttempt attempt = getUserAttempt(attemptId, userId);

        if (attempt.getStatus() == AttemptStatus.NOT_STARTED) {
            throw new BadRequestException("Quiz attempt has not been started");
        }

        return attempt.getQuiz().getQuestions()
                .stream()
                .sorted(Comparator.comparing(Question::getId))
                .map(question -> AttemptQuestionResponse.builder()
                        .id(question.getId())
                        .content(question.getContent())
                        .points(question.getPoints())
                        .options(question.getOptions()
                                .stream()
                                .sorted(Comparator.comparing(Option::getId))
                                .map(option -> AttemptOptionResponse.builder()
                                        .id(option.getId())
                                        .content(option.getContent())
                                        .build())
                                .toList())
                        .build())
                .toList();
    }

    @Transactional
    public SubmitQuizResponse submitQuiz(Long attemptId, Long userId, SubmitQuizRequest request) {
        QuizAttempt attempt = getUserAttempt(attemptId, userId);

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new BadRequestException("Only in-progress attempts can be submitted");
        }

        validateSubmittedAnswers(attempt, request);

        attempt.getAnswers().clear();

        double score = 0.0;
        int correctAnswers = 0;
        int wrongAnswers = 0;

        for (SubmitAnswerRequest submittedAnswer : request.getAnswers()) {
            Question question = attempt.getQuiz().getQuestions()
                    .stream()
                    .filter(item -> item.getId().equals(submittedAnswer.getQuestionId()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("Question does not belong to this quiz"));

            Option selectedOption = question.getOptions()
                    .stream()
                    .filter(option -> option.getId().equals(submittedAnswer.getSelectedOptionId()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("Selected option does not belong to the given question"));

            boolean correct = selectedOption.isCorrect();
            if (correct) {
                correctAnswers++;
                score += question.getPoints();
            } else {
                wrongAnswers++;
            }

            AttemptAnswer answer = new AttemptAnswer();
            answer.setQuizAttempt(attempt);
            answer.setQuestion(question);
            answer.setSelectedOption(selectedOption);
            answer.setCorrect(correct);
            attempt.getAnswers().add(answer);
        }

        attempt.setScore(score);
        attempt.setCorrectAnswers(correctAnswers);
        attempt.setWrongAnswers(wrongAnswers);
        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(LocalDateTime.now());

        return SubmitQuizResponse.builder()
                .attemptId(attempt.getId())
                .status(attempt.getStatus())
                .submittedAt(attempt.getSubmittedAt())
                .score(attempt.getScore())
                .correctAnswers(attempt.getCorrectAnswers())
                .wrongAnswers(attempt.getWrongAnswers())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AttemptHistoryResponse> getAttemptHistory(Long userId) {
        return quizAttemptRepository.findByUserId(userId)
                .stream()
                .sorted(Comparator.comparing(QuizAttempt::getCreatedAt).reversed())
                .map(this::mapAttemptHistory)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuizResultResponse> getResultHistory(Long userId) {
        return quizAttemptRepository.findByUserId(userId)
                .stream()
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
        Set<Long> allowedQuestionIds = attempt.getQuiz().getQuestions()
                .stream()
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

        if (request.getAnswers().size() != allowedQuestionIds.size()) {
            throw new BadRequestException("All questions must be answered before submitting the quiz");
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
                .questionCount(quiz.getQuestions().size())
                .build();
    }

    private QuizResultResponse mapQuizResult(QuizAttempt attempt) {
        double maxScore = calculateMaxScore(attempt.getQuiz());

        List<AttemptAnswerResultResponse> answers = attempt.getAnswers()
                .stream()
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

    private double calculateMaxScore(Quiz quiz) {
        return quiz.getQuestions()
                .stream()
                .mapToInt(Question::getPoints)
                .sum();
    }

    private double calculatePercentage(double score, double maxScore) {
        if (maxScore == 0.0) {
            return 0.0;
        }
        return (score / maxScore) * 100.0;
    }
}
