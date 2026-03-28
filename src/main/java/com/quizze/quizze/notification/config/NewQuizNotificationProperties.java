package com.quizze.quizze.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.notifications.new-quiz")
public class NewQuizNotificationProperties {

    private boolean enabled;
    private String topic = "quizze.new-quiz-published";
    private String consumerGroupId = "quizze-new-quiz-mailer";
    private int batchSize = 100;
    private int maxRetryAttempts = 2;
}
