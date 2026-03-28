package com.quizze.quizze.notification.event;

public record QuizPublishedEvent(
        Long quizId,
        String quizTitle,
        String quizDescription,
        String categoryName
) {
}
