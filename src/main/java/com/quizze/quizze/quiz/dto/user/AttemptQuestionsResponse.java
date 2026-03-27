package com.quizze.quizze.quiz.dto.user;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AttemptQuestionsResponse {

    private final Long attemptId;
    private final Long quizId;
    private final String quizTitle;
    private final LocalDateTime startedAt;
    private final LocalDateTime expiresAt;
    private final Integer timeLimitInMinutes;
    private final boolean timeExpired;
    private final List<AttemptQuestionResponse> questions;
}
