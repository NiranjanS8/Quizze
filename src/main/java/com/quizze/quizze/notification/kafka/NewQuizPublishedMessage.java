package com.quizze.quizze.notification.kafka;

public record NewQuizPublishedMessage(
        String eventId,
        Long quizId,
        String quizTitle,
        String quizDescription,
        String categoryName
) {
}
