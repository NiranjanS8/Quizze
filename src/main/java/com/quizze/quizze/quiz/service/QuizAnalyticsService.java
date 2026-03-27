package com.quizze.quizze.quiz.service;

import com.quizze.quizze.common.exception.ResourceNotFoundException;
import com.quizze.quizze.quiz.domain.AttemptStatus;
import com.quizze.quizze.quiz.domain.Quiz;
import com.quizze.quizze.quiz.domain.QuizAttempt;
import com.quizze.quizze.quiz.dto.analytics.QuizPerformanceAnalyticsResponse;
import com.quizze.quizze.quiz.repository.QuizAttemptRepository;
import com.quizze.quizze.quiz.repository.QuizRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuizAnalyticsService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    @Transactional(readOnly = true)
    public QuizPerformanceAnalyticsResponse getQuizPerformanceAnalytics(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + quizId));

        List<QuizAttempt> attempts = quizAttemptRepository.findByQuizId(quizId);
        List<QuizAttempt> submittedAttempts = attempts.stream()
                .filter(attempt -> attempt.getStatus() == AttemptStatus.SUBMITTED)
                .toList();

        double maxScore = quiz.getQuestions().stream()
                .mapToInt(question -> question.getPoints())
                .sum();

        long totalAttempts = attempts.size();
        long submittedCount = submittedAttempts.size();
        long inProgressCount = attempts.stream().filter(attempt -> attempt.getStatus() == AttemptStatus.IN_PROGRESS).count();
        long expiredCount = attempts.stream().filter(attempt -> attempt.getStatus() == AttemptStatus.EXPIRED).count();

        double averageScore = submittedAttempts.stream().mapToDouble(QuizAttempt::getScore).average().orElse(0.0);
        double averagePercentage = submittedAttempts.stream()
                .mapToDouble(attempt -> calculatePercentage(attempt.getScore(), maxScore))
                .average()
                .orElse(0.0);
        double highestScore = submittedAttempts.stream().mapToDouble(QuizAttempt::getScore).max().orElse(0.0);
        double lowestScore = submittedAttempts.stream().mapToDouble(QuizAttempt::getScore).min().orElse(0.0);
        double averageCorrectAnswers = submittedAttempts.stream().mapToInt(QuizAttempt::getCorrectAnswers).average().orElse(0.0);
        double averageWrongAnswers = submittedAttempts.stream().mapToInt(QuizAttempt::getWrongAnswers).average().orElse(0.0);
        LocalDateTime lastSubmittedAt = submittedAttempts.stream()
                .map(QuizAttempt::getSubmittedAt)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        double completionRate = totalAttempts == 0 ? 0.0 : (submittedCount * 100.0) / totalAttempts;

        return QuizPerformanceAnalyticsResponse.builder()
                .quizId(quiz.getId())
                .quizTitle(quiz.getTitle())
                .totalAttempts(totalAttempts)
                .submittedAttempts(submittedCount)
                .inProgressAttempts(inProgressCount)
                .expiredAttempts(expiredCount)
                .completionRate(completionRate)
                .averageScore(averageScore)
                .averagePercentage(averagePercentage)
                .highestScore(highestScore)
                .lowestScore(lowestScore)
                .maxScore(maxScore)
                .averageCorrectAnswers(averageCorrectAnswers)
                .averageWrongAnswers(averageWrongAnswers)
                .lastSubmittedAt(lastSubmittedAt)
                .build();
    }

    private double calculatePercentage(double score, double maxScore) {
        if (maxScore == 0.0) {
            return 0.0;
        }
        return Math.max(0.0, (score / maxScore) * 100.0);
    }
}
