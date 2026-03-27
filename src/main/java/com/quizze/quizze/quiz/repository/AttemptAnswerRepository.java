package com.quizze.quizze.quiz.repository;

import com.quizze.quizze.quiz.domain.AttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, Long> {
}
