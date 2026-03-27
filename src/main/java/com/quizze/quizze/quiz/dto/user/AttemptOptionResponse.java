package com.quizze.quizze.quiz.dto.user;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AttemptOptionResponse {

    private final Long id;
    private final String content;
}
