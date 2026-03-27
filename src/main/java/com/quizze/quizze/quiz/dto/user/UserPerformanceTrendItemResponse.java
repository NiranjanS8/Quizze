package com.quizze.quizze.quiz.dto.user;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserPerformanceTrendItemResponse {

    private final Long attemptId;
    private final String quizTitle;
    private final String categoryName;
    private final double score;
    private final double maxScore;
    private final double percentage;
    private final LocalDateTime submittedAt;
}
