package com.quizze.quizze.quiz.dto.user;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserPerformanceAnalyticsResponse {

    private final long totalSubmittedAttempts;
    private final long totalDistinctQuizzes;
    private final double averageScore;
    private final double averagePercentage;
    private final double bestPercentage;
    private final UserCategoryPerformanceResponse strongestCategory;
    private final UserCategoryPerformanceResponse weakestCategory;
    private final List<UserPerformanceTrendItemResponse> recentTrend;
}
