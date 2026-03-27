package com.quizze.quizze.quiz.repository;

import com.quizze.quizze.quiz.domain.QuizAttempt;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findByUserId(Long userId);

    List<QuizAttempt> findByQuizId(Long quizId);

    List<QuizAttempt> findByUserIdAndQuizId(Long userId, Long quizId);

    Optional<QuizAttempt> findByIdAndUserId(Long attemptId, Long userId);
}
