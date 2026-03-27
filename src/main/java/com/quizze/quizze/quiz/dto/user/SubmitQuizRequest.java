package com.quizze.quizze.quiz.dto.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmitQuizRequest {

    @Valid
    @NotEmpty(message = "At least one answer is required")
    private List<SubmitAnswerRequest> answers;
}
