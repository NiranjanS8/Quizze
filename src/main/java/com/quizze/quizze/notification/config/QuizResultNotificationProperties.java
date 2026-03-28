package com.quizze.quizze.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.notifications.quiz-result")
public class QuizResultNotificationProperties {

    private boolean enabled;
    private String topic = "quizze.quiz-submitted";
    private String consumerGroupId = "quizze-quiz-result-mailer";
    private int maxRetryAttempts = 2;
}
