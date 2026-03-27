package com.quizze.quizze.quiz.dto.analytics;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminOverviewItemResponse {

    private final Long quizId;
    private final String quizTitle;
    private final String categoryName;
    private final long attempts;
    private final double averageScore;
    private final double averagePercentage;
}
