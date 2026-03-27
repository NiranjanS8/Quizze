package com.quizze.quizze.quiz.service;

import com.quizze.quizze.quiz.domain.AttemptStatus;
import com.quizze.quizze.quiz.domain.Quiz;
import com.quizze.quizze.quiz.domain.QuizAttempt;
import com.quizze.quizze.quiz.dto.analytics.AdminOverviewItemResponse;
import com.quizze.quizze.quiz.dto.analytics.AdminOverviewResponse;
import com.quizze.quizze.quiz.repository.QuizAttemptRepository;
import com.quizze.quizze.quiz.repository.QuizRepository;
import com.quizze.quizze.user.repository.UserRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminOverviewService {

    private final UserRepository userRepository;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    @Transactional(readOnly = true)
    public AdminOverviewResponse getOverview() {
        List<Quiz> quizzes = quizRepository.findAll();
        List<QuizAttempt> attempts = quizAttemptRepository.findAll();
        List<QuizAttempt> submittedAttempts = attempts.stream()
                .filter(attempt -> attempt.getStatus() == AttemptStatus.SUBMITTED)
                .toList();

        List<AdminOverviewItemResponse> mostAttemptedQuizzes = quizzes.stream()
                .map(quiz -> buildOverviewItem(quiz, attempts))
                .sorted(Comparator.comparing(AdminOverviewItemResponse::getAttempts, Comparator.reverseOrder())
                        .thenComparing(AdminOverviewItemResponse::getQuizTitle))
                .limit(5)
                .toList();

        List<AdminOverviewItemResponse> topPerformingQuizzes = quizzes.stream()
                .map(quiz -> buildOverviewItem(quiz, submittedAttempts))
                .filter(item -> item.getAttempts() > 0)
                .sorted(Comparator.comparing(AdminOverviewItemResponse::getAveragePercentage, Comparator.reverseOrder())
                        .thenComparing(AdminOverviewItemResponse::getAttempts, Comparator.reverseOrder())
                        .thenComparing(AdminOverviewItemResponse::getQuizTitle))
                .limit(5)
                .toList();

        long totalQuizzes = quizzes.size();
        long publishedQuizzes = quizzes.stream().filter(Quiz::isPublished).count();

        return AdminOverviewResponse.builder()
                .totalUsers(userRepository.count())
                .totalQuizzes(totalQuizzes)
                .publishedQuizzes(publishedQuizzes)
                .draftQuizzes(totalQuizzes - publishedQuizzes)
                .totalAttempts(attempts.size())
                .submittedAttempts(submittedAttempts.size())
                .mostAttemptedQuizzes(mostAttemptedQuizzes)
                .topPerformingQuizzes(topPerformingQuizzes)
                .build();
    }

    private AdminOverviewItemResponse buildOverviewItem(Quiz quiz, List<QuizAttempt> attempts) {
        List<QuizAttempt> quizAttempts = attempts.stream()
                .filter(attempt -> attempt.getQuiz().getId().equals(quiz.getId()))
                .toList();

        double maxScore = quiz.getQuestions().stream().mapToInt(question -> question.getPoints()).sum();
        double averageScore = quizAttempts.stream().mapToDouble(QuizAttempt::getScore).average().orElse(0.0);
        double averagePercentage = quizAttempts.stream()
                .mapToDouble(attempt -> calculatePercentage(attempt.getScore(), maxScore))
                .average()
                .orElse(0.0);

        return AdminOverviewItemResponse.builder()
                .quizId(quiz.getId())
                .quizTitle(quiz.getTitle())
                .categoryName(quiz.getCategory() == null ? null : quiz.getCategory().getName())
                .attempts(quizAttempts.size())
                .averageScore(averageScore)
                .averagePercentage(averagePercentage)
                .build();
    }

    private double calculatePercentage(double score, double maxScore) {
        if (maxScore == 0.0) {
            return 0.0;
        }
        return Math.max(0.0, (score / maxScore) * 100.0);
    }
}
