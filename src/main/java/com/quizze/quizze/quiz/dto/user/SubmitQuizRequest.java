package com.quizze.quizze.quiz.dto.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmitQuizRequest {

    @Valid
    @NotEmpty(message = "At least one answer is required")
    @Size(max = 100, message = "A submission cannot contain more than 100 answers")
    private List<SubmitAnswerRequest> answers;
}
