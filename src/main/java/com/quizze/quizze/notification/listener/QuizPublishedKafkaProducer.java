package com.quizze.quizze.notification.listener;

import com.quizze.quizze.notification.config.NewQuizNotificationProperties;
import com.quizze.quizze.notification.event.QuizPublishedEvent;
import com.quizze.quizze.notification.kafka.NewQuizPublishedMessage;
import com.quizze.quizze.monitoring.service.ApplicationMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notifications.new-quiz", name = "enabled", havingValue = "true")
public class QuizPublishedKafkaProducer {

    private final KafkaTemplate<String, NewQuizPublishedMessage> kafkaTemplate;
    private final NewQuizNotificationProperties properties;
    private final ApplicationMetricsService metricsService;

    @EventListener
    public void handleQuizPublished(QuizPublishedEvent event) {
        NewQuizPublishedMessage message = new NewQuizPublishedMessage(
                event.quizId(),
                event.quizTitle(),
                event.quizDescription(),
                event.categoryName()
        );

        kafkaTemplate.send(properties.getTopic(), String.valueOf(event.quizId()), message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        metricsService.increment("quizze.kafka.new_quiz.publish.failed");
                        log.warn("Failed to publish new quiz notification event for quizId={}. Reason: {}", event.quizId(), ex.getMessage());
                    } else {
                        metricsService.increment("quizze.kafka.new_quiz.publish.success");
                        log.info("Published new quiz notification event to Kafka for quizId={}", event.quizId());
                    }
                });
    }
}
