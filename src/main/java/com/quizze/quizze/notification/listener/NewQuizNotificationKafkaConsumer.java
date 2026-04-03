package com.quizze.quizze.notification.listener;

import com.quizze.quizze.notification.config.NewQuizNotificationProperties;
import com.quizze.quizze.notification.kafka.NewQuizPublishedMessage;
import com.quizze.quizze.notification.service.NewQuizNotificationEmailService;
import com.quizze.quizze.notification.service.KafkaEventDeduplicationService;
import com.quizze.quizze.monitoring.service.ApplicationMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notifications.new-quiz", name = "enabled", havingValue = "true")
public class NewQuizNotificationKafkaConsumer {

    private static final String CONSUMER_NAME = "new-quiz-email-consumer";

    private final NewQuizNotificationEmailService newQuizNotificationEmailService;
    private final KafkaEventDeduplicationService deduplicationService;
    private final ApplicationMetricsService metricsService;
    private final NewQuizNotificationProperties properties;

    @RetryableTopic(
            attempts = "#{__listener.properties.consumerDeliveryAttempts}",
            backoff = @Backoff(delayExpression = "#{__listener.properties.consumerRetryDelayMs}"),
            dltStrategy = DltStrategy.ALWAYS_RETRY_ON_ERROR
    )
    @KafkaListener(
            topics = "${app.notifications.new-quiz.topic}",
            groupId = "${app.notifications.new-quiz.consumer-group-id}"
    )
    public void consume(@Payload NewQuizPublishedMessage message) {
        try {
            if (deduplicationService.isAlreadyProcessed(message.eventId(), CONSUMER_NAME)) {
                metricsService.increment("quizze.kafka.new_quiz.consume.duplicate");
                log.info("Skipping duplicate new quiz notification eventId={} for quizId={}", message.eventId(), message.quizId());
                return;
            }

            metricsService.increment("quizze.kafka.new_quiz.consume.success");
            log.info("Consuming new quiz notification event for quizId={}", message.quizId());
            newQuizNotificationEmailService.notifyOptedInUsers(message);
            deduplicationService.markProcessed(message.eventId(), "NEW_QUIZ_PUBLISHED", CONSUMER_NAME);
        } catch (Exception ex) {
            metricsService.increment("quizze.kafka.new_quiz.consume.failed");
            log.warn(
                    "New quiz notification consumer failed for eventId={} quizId={}. Reason: {}",
                    message.eventId(),
                    message.quizId(),
                    ex.getMessage()
            );
            throw new IllegalStateException("New quiz notification processing failed", ex);
        }
    }

    @DltHandler
    public void handleDlt(
            @Payload NewQuizPublishedMessage message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic
    ) {
        metricsService.increment("quizze.kafka.new_quiz.dlt");
        log.error("New quiz notification message moved to DLT topic={} for eventId={} quizId={}", topic, message.eventId(), message.quizId());
    }
}
