package com.quizze.quizze.quiz.dto.user;

import com.quizze.quizze.quiz.domain.AttemptStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StartQuizResponse {

    private final Long attemptId;
    private final Long quizId;
    private final String quizTitle;
    private final AttemptStatus status;
    private final LocalDateTime startedAt;
    private final Integer questionCount;
}
