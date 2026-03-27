package com.quizze.quizze.quiz.dto.leaderboard;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuizLeaderboardEntryResponse {

    private final Integer rank;
    private final Long userId;
    private final String username;
    private final Double score;
    private final Double maxScore;
    private final Double percentage;
    private final Integer correctAnswers;
    private final Integer wrongAnswers;
    private final LocalDateTime submittedAt;
}
