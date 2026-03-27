package com.quizze.quizze.quiz.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmitAnswerRequest {

    @NotNull(message = "Question id is required")
    private Long questionId;

    @NotNull(message = "Selected option id is required")
    private Long selectedOptionId;
}
