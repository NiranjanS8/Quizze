package com.quizze.quizze.quiz.dto.analytics;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuestionAnalyticsResponse {

    private final Long quizId;
    private final String quizTitle;
    private final List<QuestionAnalyticsItemResponse> hardestQuestions;
    private final List<QuestionAnalyticsItemResponse> easiestQuestions;
}
