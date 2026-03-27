package com.quizze.quizze.quiz.dto.admin;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OptionResponse {

    private final Long id;
    private final String content;
    private final boolean correct;
}
