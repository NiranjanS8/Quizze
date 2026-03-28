package com.quizze.quizze.quiz.listener;

import com.quizze.quizze.notification.config.QuizResultNotificationProperties;
import com.quizze.quizze.notification.kafka.QuizSubmittedMessage;
import com.quizze.quizze.monitoring.service.ApplicationMetricsService;
import com.quizze.quizze.quiz.event.QuizSubmittedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notifications.quiz-result", name = "enabled", havingValue = "true")
public class QuizSubmittedKafkaProducer {

    private final KafkaTemplate<String, QuizSubmittedMessage> kafkaTemplate;
    private final QuizResultNotificationProperties properties;
    private final ApplicationMetricsService metricsService;

    @EventListener
    public void handleQuizSubmitted(QuizSubmittedEvent event) {
        QuizSubmittedMessage message = new QuizSubmittedMessage(event.attemptId(), event.quizId(), event.userId());
        kafkaTemplate.send(properties.getTopic(), String.valueOf(event.attemptId()), message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        metricsService.increment("quizze.kafka.quiz_result.publish.failed");
                        log.warn("Failed to publish quiz submitted message for attemptId={}. Reason: {}", event.attemptId(), ex.getMessage());
                    } else {
                        metricsService.increment("quizze.kafka.quiz_result.publish.success");
                        log.info("Published quiz submitted message to Kafka for attemptId={}", event.attemptId());
                    }
                });
    }
}
