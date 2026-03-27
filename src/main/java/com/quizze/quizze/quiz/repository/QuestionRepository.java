package com.quizze.quizze.quiz.repository;

import com.quizze.quizze.quiz.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {
}
