package com.quizze.quizze.quiz.dto.user;

import com.quizze.quizze.quiz.domain.AttemptStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuizResultResponse {

    private final Long attemptId;
    private final Long quizId;
    private final String quizTitle;
    private final AttemptStatus status;
    private final LocalDateTime startedAt;
    private final LocalDateTime submittedAt;
    private final Integer totalQuestions;
    private final Integer attemptedQuestions;
    private final Integer correctAnswers;
    private final Integer wrongAnswers;
    private final Double score;
    private final Double maxScore;
    private final Double percentage;
    private final List<AttemptAnswerResultResponse> answers;
}
