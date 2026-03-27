package com.quizze.quizze.quiz.dto.user;

import com.quizze.quizze.quiz.domain.DifficultyLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuizSummaryResponse {

    private final Long id;
    private final String title;
    private final String description;
    private final String categoryName;
    private final DifficultyLevel difficulty;
    private final Integer timeLimitInMinutes;
    private final Integer questionCount;
    private final boolean oneAttemptOnly;
    private final boolean negativeMarkingEnabled;
}
