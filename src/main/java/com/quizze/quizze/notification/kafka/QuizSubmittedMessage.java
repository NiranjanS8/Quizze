package com.quizze.quizze.notification.kafka;

public record QuizSubmittedMessage(String eventId, Long attemptId, Long quizId, Long userId) {
}
