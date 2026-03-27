package com.quizze.quizze.quiz.dto.user;

import com.quizze.quizze.quiz.domain.AttemptStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubmitQuizResponse {

    private final Long attemptId;
    private final AttemptStatus status;
    private final LocalDateTime submittedAt;
    private final Double score;
    private final Integer correctAnswers;
    private final Integer wrongAnswers;
}
