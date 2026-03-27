package com.quizze.quizze.quiz.repository;

import com.quizze.quizze.quiz.domain.Quiz;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findByPublishedTrue();

    Optional<Quiz> findByIdAndPublishedTrue(Long id);
}
