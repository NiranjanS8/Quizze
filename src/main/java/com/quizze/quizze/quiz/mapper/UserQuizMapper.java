package com.quizze.quizze.quiz.mapper;

import com.quizze.quizze.quiz.domain.AttemptAnswer;
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
import com.quizze.quizze.quiz.dto.user.SubmitQuizResponse;
import com.quizze.quizze.quiz.dto.user.UserCategoryPerformanceResponse;
import com.quizze.quizze.quiz.dto.user.UserPerformanceAnalyticsResponse;
import com.quizze.quizze.quiz.dto.user.UserPerformanceTrendItemResponse;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class UserQuizMapper {

    public QuizCatalogResponse toQuizCatalogResponse(Page<Quiz> quizPage, List<String> availableCategories) {
        return QuizCatalogResponse.builder()
                .content(quizPage.getContent().stream().map(this::toQuizSummaryResponse).toList())
                .availableCategories(availableCategories)
                .pageNumber(quizPage.getNumber())
                .pageSize(quizPage.getSize())
                .totalPages(quizPage.getTotalPages())
                .totalElements(quizPage.getTotalElements())
                .hasNext(quizPage.hasNext())
                .hasPrevious(quizPage.hasPrevious())
                .build();
    }

    public StartQuizResponse toStartQuizResponse(QuizAttempt attempt, LocalDateTime expiresAt) {
        return StartQuizResponse.builder()
                .attemptId(attempt.getId())
                .quizId(attempt.getQuiz().getId())
                .quizTitle(attempt.getQuiz().getTitle())
                .status(attempt.getStatus())
                .startedAt(attempt.getStartedAt())
                .expiresAt(expiresAt)
                .timeLimitInMinutes(attempt.getQuiz().getTimeLimitInMinutes())
                .questionCount(attempt.getQuiz().getQuestions().size())
                .build();
    }

    public AttemptQuestionsResponse toAttemptQuestionsResponse(
            QuizAttempt attempt,
            LocalDateTime expiresAt,
            boolean timeExpired,
            List<Question> orderedQuestions
    ) {
        return AttemptQuestionsResponse.builder()
                .attemptId(attempt.getId())
                .quizId(attempt.getQuiz().getId())
                .quizTitle(attempt.getQuiz().getTitle())
                .startedAt(attempt.getStartedAt())
                .expiresAt(expiresAt)
                .timeLimitInMinutes(attempt.getQuiz().getTimeLimitInMinutes())
                .timeExpired(timeExpired)
                .questions(orderedQuestions.stream().map(this::toAttemptQuestionResponse).toList())
                .build();
    }

    public SubmitQuizResponse toSubmitQuizResponse(
            QuizAttempt attempt,
            double maxScore,
            double percentage,
            boolean timeExpired
    ) {
        return SubmitQuizResponse.builder()
                .attemptId(attempt.getId())
                .status(attempt.getStatus())
                .submittedAt(attempt.getSubmittedAt())
                .score(attempt.getScore())
                .maxScore(maxScore)
                .percentage(percentage)
                .correctAnswers(attempt.getCorrectAnswers())
                .wrongAnswers(attempt.getWrongAnswers())
                .timeExpired(timeExpired)
                .build();
    }

    public AttemptHistoryResponse toAttemptHistoryResponse(QuizAttempt attempt, double maxScore, double percentage) {
        return AttemptHistoryResponse.builder()
                .attemptId(attempt.getId())
                .quizId(attempt.getQuiz().getId())
                .quizTitle(attempt.getQuiz().getTitle())
                .status(attempt.getStatus())
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .score(attempt.getScore())
                .maxScore(maxScore)
                .percentage(percentage)
                .correctAnswers(attempt.getCorrectAnswers())
                .wrongAnswers(attempt.getWrongAnswers())
                .build();
    }

    public QuizDetailResponse toQuizDetailResponse(Quiz quiz) {
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

    public QuizResultResponse toQuizResultResponse(QuizAttempt attempt, double maxScore, double percentage) {
        List<AttemptAnswerResultResponse> answers = attempt.getAnswers().stream()
                .sorted(Comparator.comparing(answer -> answer.getQuestion().getId()))
                .map(this::toAttemptAnswerResultResponse)
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
                .percentage(percentage)
                .answers(answers)
                .build();
    }

    public UserCategoryPerformanceResponse toUserCategoryPerformanceResponse(
            String categoryName,
            int attempts,
            double averageScore,
            double averagePercentage
    ) {
        return UserCategoryPerformanceResponse.builder()
                .categoryName(categoryName)
                .attempts(attempts)
                .averageScore(averageScore)
                .averagePercentage(averagePercentage)
                .build();
    }

    public UserPerformanceTrendItemResponse toUserPerformanceTrendItemResponse(
            QuizAttempt attempt,
            String categoryName,
            double maxScore,
            double percentage
    ) {
        return UserPerformanceTrendItemResponse.builder()
                .attemptId(attempt.getId())
                .quizTitle(attempt.getQuiz().getTitle())
                .categoryName(categoryName)
                .score(attempt.getScore())
                .maxScore(maxScore)
                .percentage(percentage)
                .submittedAt(attempt.getSubmittedAt())
                .build();
    }

    public UserPerformanceAnalyticsResponse toUserPerformanceAnalyticsResponse(
            int totalSubmittedAttempts,
            long totalDistinctQuizzes,
            double averageScore,
            double averagePercentage,
            double bestPercentage,
            UserCategoryPerformanceResponse strongestCategory,
            UserCategoryPerformanceResponse weakestCategory,
            List<UserPerformanceTrendItemResponse> recentTrend
    ) {
        return UserPerformanceAnalyticsResponse.builder()
                .totalSubmittedAttempts(totalSubmittedAttempts)
                .totalDistinctQuizzes(totalDistinctQuizzes)
                .averageScore(averageScore)
                .averagePercentage(averagePercentage)
                .bestPercentage(bestPercentage)
                .strongestCategory(strongestCategory)
                .weakestCategory(weakestCategory)
                .recentTrend(recentTrend)
                .build();
    }

    private QuizSummaryResponse toQuizSummaryResponse(Quiz quiz) {
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

    private AttemptQuestionResponse toAttemptQuestionResponse(Question question) {
        return AttemptQuestionResponse.builder()
                .id(question.getId())
                .content(question.getContent())
                .points(question.getPoints())
                .options(question.getOptions().stream()
                        .sorted(Comparator.comparing(Option::getId))
                        .map(this::toAttemptOptionResponse)
                        .toList())
                .build();
    }

    private AttemptOptionResponse toAttemptOptionResponse(Option option) {
        return AttemptOptionResponse.builder()
                .id(option.getId())
                .content(option.getContent())
                .build();
    }

    private AttemptAnswerResultResponse toAttemptAnswerResultResponse(AttemptAnswer answer) {
        return AttemptAnswerResultResponse.builder()
                .questionId(answer.getQuestion().getId())
                .questionContent(answer.getQuestion().getContent())
                .selectedOptionId(answer.getSelectedOption() == null ? null : answer.getSelectedOption().getId())
                .selectedOptionContent(answer.getSelectedOption() == null ? null : answer.getSelectedOption().getContent())
                .correct(Boolean.TRUE.equals(answer.getCorrect()))
                .points(answer.getQuestion().getPoints())
                .build();
    }
}
