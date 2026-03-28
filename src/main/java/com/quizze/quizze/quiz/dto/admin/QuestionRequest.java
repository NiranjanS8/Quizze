package com.quizze.quizze.quiz.dto.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionRequest {

    @NotBlank(message = "Question content is required")
    @Size(max = 1000, message = "Question content must not exceed 1000 characters")
    private String content;

    @NotNull(message = "Points are required")
    @Min(value = 1, message = "Points must be at least 1")
    @Max(value = 100, message = "Points must not exceed 100")
    private Integer points = 1;

    @Valid
    @NotEmpty(message = "At least two options are required")
    @Size(min = 2, max = 6, message = "A question must have between 2 and 6 options")
    private List<OptionRequest> options;
}
