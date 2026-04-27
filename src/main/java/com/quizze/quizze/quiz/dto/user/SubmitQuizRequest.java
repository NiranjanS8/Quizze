package com.quizze.quizze.quiz.dto.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmitQuizRequest {

    @Valid
    @NotNull(message = "Answers are required")
    @Size(max = 100, message = "A submission cannot contain more than 100 answers")
    private List<SubmitAnswerRequest> answers;
}
