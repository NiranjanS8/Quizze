package com.quizze.quizze.quiz.repository;

import com.quizze.quizze.quiz.domain.AttemptAnswer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, Long> {

    List<AttemptAnswer> findByQuestionQuizId(Long quizId);
}
