package com.quizze.quizze.quiz.repository;

import com.quizze.quizze.quiz.domain.QuizAnalyticsProjection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizAnalyticsProjectionRepository extends JpaRepository<QuizAnalyticsProjection, Long> {

    Optional<QuizAnalyticsProjection> findByQuizId(Long quizId);

    List<QuizAnalyticsProjection> findAllBySubmittedAttemptsGreaterThan(long submittedAttempts);
}
