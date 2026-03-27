package com.quizze.quizze.quiz.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OptionRequest {

    @NotBlank(message = "Option content is required")
    @Size(max = 500, message = "Option content must not exceed 500 characters")
    private String content;

    private boolean correct;
}
