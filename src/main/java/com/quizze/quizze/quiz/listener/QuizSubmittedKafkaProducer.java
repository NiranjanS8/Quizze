package com.quizze.quizze.quiz.listener;

import com.quizze.quizze.notification.config.QuizResultNotificationProperties;
import com.quizze.quizze.notification.kafka.QuizSubmittedMessage;
import com.quizze.quizze.monitoring.service.ApplicationMetricsService;
import com.quizze.quizze.quiz.event.QuizSubmittedEvent;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notifications.quiz-result", name = "enabled", havingValue = "true")
public class QuizSubmittedKafkaProducer {

    private final KafkaTemplate<String, QuizSubmittedMessage> kafkaTemplate;
    private final QuizResultNotificationProperties properties;
    private final ApplicationMetricsService metricsService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleQuizSubmitted(QuizSubmittedEvent event) {
        QuizSubmittedMessage message = new QuizSubmittedMessage(
                UUID.randomUUID().toString(),
                event.attemptId(),
                event.quizId(),
                event.userId()
        );
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
