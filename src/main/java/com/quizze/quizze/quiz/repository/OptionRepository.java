package com.quizze.quizze.quiz.repository;

import com.quizze.quizze.quiz.domain.Option;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OptionRepository extends JpaRepository<Option, Long> {
}
