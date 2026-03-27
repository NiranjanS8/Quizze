package com.quizze.quizze.quiz.dto.user;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserCategoryPerformanceResponse {

    private final String categoryName;
    private final long attempts;
    private final double averageScore;
    private final double averagePercentage;
}
