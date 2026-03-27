package com.quizze.quizze.quiz.dto.admin;

import com.quizze.quizze.quiz.domain.DifficultyLevel;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuizResponse {

    private final Long id;
    private final String title;
    private final String description;
    private final String categoryName;
    private final DifficultyLevel difficulty;
    private final Integer timeLimitInMinutes;
    private final boolean published;
    private final boolean negativeMarkingEnabled;
    private final boolean oneAttemptOnly;
    private final List<QuestionResponse> questions;
}
