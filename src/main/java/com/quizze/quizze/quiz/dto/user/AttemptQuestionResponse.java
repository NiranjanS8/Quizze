package com.quizze.quizze.quiz.dto.user;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AttemptQuestionResponse {

    private final Long id;
    private final String content;
    private final Integer points;
    private final List<AttemptOptionResponse> options;
}
