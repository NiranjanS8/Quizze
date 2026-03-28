package com.quizze.quizze.quiz.event;

public record QuizSubmittedEvent(Long quizId, Long userId, Long attemptId) {
}
