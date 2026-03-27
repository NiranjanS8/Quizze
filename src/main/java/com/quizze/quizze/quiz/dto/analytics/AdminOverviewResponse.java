package com.quizze.quizze.quiz.dto.analytics;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminOverviewResponse {

    private final long totalUsers;
    private final long totalQuizzes;
    private final long publishedQuizzes;
    private final long draftQuizzes;
    private final long totalAttempts;
    private final long submittedAttempts;
    private final List<AdminOverviewItemResponse> mostAttemptedQuizzes;
    private final List<AdminOverviewItemResponse> topPerformingQuizzes;
}
