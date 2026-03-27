package com.quizze.quizze.quiz.dto.admin;

import com.quizze.quizze.quiz.domain.DifficultyLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuizRequest {

    @NotBlank(message = "Quiz title is required")
    @Size(max = 150, message = "Quiz title must not exceed 150 characters")
    private String title;

    @Size(max = 1000, message = "Quiz description must not exceed 1000 characters")
    private String description;

    @Size(max = 100, message = "Category name must not exceed 100 characters")
    private String categoryName;

    @NotNull(message = "Difficulty is required")
    private DifficultyLevel difficulty;

    @Min(value = 0, message = "Time limit must be zero or greater")
    @Max(value = 300, message = "Time limit must not exceed 300 minutes")
    private Integer timeLimitInMinutes;

    private boolean published;

    private boolean negativeMarkingEnabled;

    private boolean oneAttemptOnly;
}
