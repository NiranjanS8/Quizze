package com.quizze.quizze.quiz.dto.analytics;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuestionAnalyticsItemResponse {

    private final Long questionId;
    private final String questionContent;
    private final Integer points;
    private final long totalAnswers;
    private final long correctAnswers;
    private final long wrongAnswers;
    private final double correctPercentage;
    private final double wrongPercentage;
}
