package com.quizze.quizze.quiz.dto.leaderboard;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuizLeaderboardResponse {

    private final Long quizId;
    private final String quizTitle;
    private final int totalSubmittedAttempts;
    private final int returnedEntries;
    private final List<QuizLeaderboardEntryResponse> entries;
}
