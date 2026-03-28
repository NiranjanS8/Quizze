package com.quizze.quizze.quiz.dto.user;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AttemptAnswerResultResponse {

    private final Long questionId;
    private final String questionContent;
    private final Long selectedOptionId;
    private final String selectedOptionContent;
    private final Long correctOptionId;
    private final String correctOptionContent;
    private final boolean correct;
    private final Integer points;
}
