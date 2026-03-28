package com.quizze.quizze.notification.kafka;

public record QuizSubmittedMessage(Long attemptId, Long quizId, Long userId) {
}
