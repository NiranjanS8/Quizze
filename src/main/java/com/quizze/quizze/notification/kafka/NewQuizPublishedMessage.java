package com.quizze.quizze.notification.kafka;

public record NewQuizPublishedMessage(
        Long quizId,
        String quizTitle,
        String quizDescription,
        String categoryName
) {
}
