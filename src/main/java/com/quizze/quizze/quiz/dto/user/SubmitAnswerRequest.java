package com.quizze.quizze.quiz.dto.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmitAnswerRequest {

    @NotNull(message = "Question id is required")
    @Positive(message = "Question id must be greater than 0")
    private Long questionId;

    @NotNull(message = "Selected option id is required")
    @Positive(message = "Selected option id must be greater than 0")
    private Long selectedOptionId;
}
