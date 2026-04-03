package com.quizze.quizze.quiz.service;

import com.quizze.quizze.common.exception.ResourceNotFoundException;
import com.quizze.quizze.quiz.domain.AttemptStatus;
import com.quizze.quizze.quiz.domain.Quiz;
import com.quizze.quizze.quiz.domain.QuizAnalyticsProjection;
import com.quizze.quizze.quiz.domain.QuizAttempt;
import com.quizze.quizze.quiz.repository.QuizAnalyticsProjectionRepository;
import com.quizze.quizze.quiz.repository.QuizAttemptRepository;
import com.quizze.quizze.quiz.repository.QuizRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class QuizAnalyticsProjectionService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizAnalyticsProjectionRepository quizAnalyticsProjectionRepository;

    @Transactional
    public void rebuildForQuiz(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + quizId));

        List<QuizAttempt> submittedAttempts = quizAttemptRepository.findByQuizIdAndStatus(quizId, AttemptStatus.SUBMITTED);
        double maxScore = quiz.getQuestions().stream().mapToInt(question -> question.getPoints()).sum();

        QuizAnalyticsProjection projection = quizAnalyticsProjectionRepository.findByQuizId(quizId)
                .orElseGet(QuizAnalyticsProjection::new);

        projection.setQuizId(quiz.getId());
        projection.setQuizTitle(quiz.getTitle());
        projection.setCategoryName(quiz.getCategory() == null ? null : quiz.getCategory().getName());
        projection.setSubmittedAttempts(submittedAttempts.size());
        projection.setAverageScore(submittedAttempts.stream().mapToDouble(QuizAttempt::getScore).average().orElse(0.0));
        projection.setAveragePercentage(submittedAttempts.stream()
                .mapToDouble(attempt -> maxScore == 0.0 ? 0.0 : Math.max(0.0, (attempt.getScore() / maxScore) * 100.0))
                .average()
                .orElse(0.0));
        projection.setHighestScore(submittedAttempts.stream().mapToDouble(QuizAttempt::getScore).max().orElse(0.0));
        projection.setLowestScore(submittedAttempts.stream().mapToDouble(QuizAttempt::getScore).min().orElse(0.0));
        projection.setMaxScore(maxScore);
        projection.setAverageCorrectAnswers(submittedAttempts.stream().mapToInt(QuizAttempt::getCorrectAnswers).average().orElse(0.0));
        projection.setAverageWrongAnswers(submittedAttempts.stream().mapToInt(QuizAttempt::getWrongAnswers).average().orElse(0.0));
        projection.setLastSubmittedAt(submittedAttempts.stream()
                .map(QuizAttempt::getSubmittedAt)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse((LocalDateTime) null));

        quizAnalyticsProjectionRepository.save(projection);
        log.info("Quiz analytics projection rebuilt for quizId={} with submittedAttempts={}", quizId, submittedAttempts.size());
    }
}
