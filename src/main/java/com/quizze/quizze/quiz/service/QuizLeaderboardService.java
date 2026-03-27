package com.quizze.quizze.quiz.service;

import com.quizze.quizze.common.exception.ResourceNotFoundException;
import com.quizze.quizze.quiz.domain.AttemptStatus;
import com.quizze.quizze.quiz.domain.Quiz;
import com.quizze.quizze.quiz.domain.QuizAttempt;
import com.quizze.quizze.quiz.dto.leaderboard.QuizLeaderboardEntryResponse;
import com.quizze.quizze.quiz.dto.leaderboard.QuizLeaderboardResponse;
import com.quizze.quizze.quiz.repository.QuizAttemptRepository;
import com.quizze.quizze.quiz.repository.QuizRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuizLeaderboardService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    @Transactional(readOnly = true)
    public QuizLeaderboardResponse getLeaderboard(Long quizId, int limit, boolean requirePublished) {
        Quiz quiz = requirePublished ? getPublishedQuiz(quizId) : getQuiz(quizId);
        int normalizedLimit = Math.min(Math.max(limit, 1), 50);

        List<QuizAttempt> submittedAttempts = quizAttemptRepository.findByQuizIdAndStatus(quizId, AttemptStatus.SUBMITTED)
                .stream()
                .sorted(Comparator.comparing(QuizAttempt::getScore, Comparator.reverseOrder())
                        .thenComparing(QuizAttempt::getSubmittedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(QuizAttempt::getId))
                .toList();

        double maxScore = quiz.getQuestions().stream()
                .mapToInt(question -> question.getPoints())
                .sum();

        List<QuizLeaderboardEntryResponse> entries = submittedAttempts.stream()
                .limit(normalizedLimit)
                .map(attempt -> mapEntry(attempt, maxScore, submittedAttempts))
                .toList();

        return QuizLeaderboardResponse.builder()
                .quizId(quiz.getId())
                .quizTitle(quiz.getTitle())
                .totalSubmittedAttempts(submittedAttempts.size())
                .returnedEntries(entries.size())
                .entries(entries)
                .build();
    }

    private QuizLeaderboardEntryResponse mapEntry(QuizAttempt attempt, double maxScore, List<QuizAttempt> orderedAttempts) {
        int rank = orderedAttempts.indexOf(attempt) + 1;
        double percentage = maxScore == 0.0 ? 0.0 : (attempt.getScore() / maxScore) * 100.0;

        return QuizLeaderboardEntryResponse.builder()
                .rank(rank)
                .userId(attempt.getUser().getId())
                .username(attempt.getUser().getUsername())
                .score(attempt.getScore())
                .maxScore(maxScore)
                .percentage(Math.max(0.0, percentage))
                .correctAnswers(attempt.getCorrectAnswers())
                .wrongAnswers(attempt.getWrongAnswers())
                .submittedAt(attempt.getSubmittedAt())
                .build();
    }

    private Quiz getQuiz(Long quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + quizId));
    }

    private Quiz getPublishedQuiz(Long quizId) {
        return quizRepository.findByIdAndPublishedTrue(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Published quiz not found with id: " + quizId));
    }
}
