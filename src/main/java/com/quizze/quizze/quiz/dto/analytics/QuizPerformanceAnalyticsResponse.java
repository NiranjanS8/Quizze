package com.quizze.quizze.quiz.dto.analytics;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuizPerformanceAnalyticsResponse {

    private final Long quizId;
    private final String quizTitle;
    private final long totalAttempts;
    private final long submittedAttempts;
    private final long inProgressAttempts;
    private final long expiredAttempts;
    private final double completionRate;
    private final double averageScore;
    private final double averagePercentage;
    private final double highestScore;
    private final double lowestScore;
    private final double maxScore;
    private final double averageCorrectAnswers;
    private final double averageWrongAnswers;
    private final LocalDateTime lastSubmittedAt;
}
