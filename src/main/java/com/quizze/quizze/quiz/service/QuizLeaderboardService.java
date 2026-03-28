package com.quizze.quizze.quiz.service;

import static com.quizze.quizze.cache.config.CacheConfig.QUIZ_LEADERBOARD_CACHE;

import com.quizze.quizze.common.exception.ResourceNotFoundException;
import com.quizze.quizze.quiz.domain.AttemptStatus;
import com.quizze.quizze.quiz.domain.Quiz;
import com.quizze.quizze.quiz.domain.QuizAttempt;
import com.quizze.quizze.quiz.dto.leaderboard.QuizLeaderboardEntryResponse;
import com.quizze.quizze.quiz.dto.leaderboard.QuizLeaderboardResponse;
import com.quizze.quizze.quiz.repository.QuizAttemptRepository;
import com.quizze.quizze.quiz.repository.QuizRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class QuizLeaderboardService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = QUIZ_LEADERBOARD_CACHE, key = "#quizId + ':' + #limit + ':' + #requirePublished")
    public QuizLeaderboardResponse getLeaderboard(Long quizId, int limit, boolean requirePublished) {
        log.debug("Fetching leaderboard for quizId={}, limit={}, requirePublished={}", quizId, limit, requirePublished);
        Quiz quiz = requirePublished ? getPublishedQuiz(quizId) : getQuiz(quizId);
        int normalizedLimit = Math.min(Math.max(limit, 1), 50);

        List<QuizAttempt> rankedAttempts = quizAttemptRepository.findByQuizIdAndStatus(quizId, AttemptStatus.SUBMITTED)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        attempt -> attempt.getUser().getId(),
                        attempt -> attempt,
                        this::pickBetterAttempt,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .sorted(leaderboardComparator())
                .toList();

        double maxScore = quiz.getQuestions().stream()
                .mapToInt(question -> question.getPoints())
                .sum();

        List<QuizLeaderboardEntryResponse> entries = rankedAttempts.stream()
                .limit(normalizedLimit)
                .map(attempt -> mapEntry(attempt, maxScore, rankedAttempts))
                .toList();
        log.debug("Leaderboard built for quizId={} with {} ranked users and {} returned entries", quizId, rankedAttempts.size(), entries.size());

        return QuizLeaderboardResponse.builder()
                .quizId(quiz.getId())
                .quizTitle(quiz.getTitle())
                .totalSubmittedAttempts(rankedAttempts.size())
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

    private QuizAttempt pickBetterAttempt(QuizAttempt current, QuizAttempt candidate) {
        Comparator<QuizAttempt> comparator = leaderboardComparator();
        return comparator.compare(current, candidate) <= 0 ? current : candidate;
    }

    private Comparator<QuizAttempt> leaderboardComparator() {
        return Comparator.comparing(QuizAttempt::getScore, Comparator.reverseOrder())
                .thenComparing(this::calculatePercentage, Comparator.reverseOrder())
                .thenComparing(QuizAttempt::getSubmittedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(QuizAttempt::getId);
    }

    private Double calculatePercentage(QuizAttempt attempt) {
        double maxScore = attempt.getQuiz().getQuestions().stream()
                .mapToInt(question -> question.getPoints())
                .sum();
        if (maxScore == 0.0) {
            return 0.0;
        }
        return Math.max(0.0, (attempt.getScore() / maxScore) * 100.0);
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
