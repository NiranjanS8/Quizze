package com.quizze.quizze.notification.listener;

import com.quizze.quizze.notification.config.QuizResultNotificationProperties;
import com.quizze.quizze.notification.kafka.QuizSubmittedMessage;
import com.quizze.quizze.notification.service.KafkaEventDeduplicationService;
import com.quizze.quizze.notification.service.QuizResultEmailService;
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
@ConditionalOnProperty(prefix = "app.notifications.quiz-result", name = "enabled", havingValue = "true")
public class QuizResultKafkaConsumer {

    private static final String CONSUMER_NAME = "quiz-result-email-consumer";

    private final QuizResultEmailService quizResultEmailService;
    private final KafkaEventDeduplicationService deduplicationService;
    private final ApplicationMetricsService metricsService;
    private final QuizResultNotificationProperties properties;

    @RetryableTopic(
            attempts = "#{__listener.properties.consumerDeliveryAttempts}",
            backoff = @Backoff(delayExpression = "#{__listener.properties.consumerRetryDelayMs}"),
            dltStrategy = DltStrategy.ALWAYS_RETRY_ON_ERROR
    )
    @KafkaListener(
            topics = "${app.notifications.quiz-result.topic}",
            groupId = "${app.notifications.quiz-result.consumer-group-id}"
    )
    public void consume(@Payload QuizSubmittedMessage message) {
        try {
            if (deduplicationService.isAlreadyProcessed(message.eventId(), CONSUMER_NAME)) {
                metricsService.increment("quizze.kafka.quiz_result.consume.duplicate");
                log.info("Skipping duplicate quiz result eventId={} for attemptId={}", message.eventId(), message.attemptId());
                return;
            }

            metricsService.increment("quizze.kafka.quiz_result.consume.success");
            log.info("Consuming quiz submitted message for attemptId={}", message.attemptId());
            quizResultEmailService.sendResultSummary(message);
            deduplicationService.markProcessed(message.eventId(), "QUIZ_SUBMITTED", CONSUMER_NAME);
        } catch (Exception ex) {
            metricsService.increment("quizze.kafka.quiz_result.consume.failed");
            log.warn(
                    "Quiz result consumer failed for eventId={} attemptId={}. Reason: {}",
                    message.eventId(),
                    message.attemptId(),
                    ex.getMessage()
            );
            throw new IllegalStateException("Quiz result notification processing failed", ex);
        }
    }

    @DltHandler
    public void handleDlt(
            @Payload QuizSubmittedMessage message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic
    ) {
        metricsService.increment("quizze.kafka.quiz_result.dlt");
        log.error("Quiz result message moved to DLT topic={} for eventId={} attemptId={}", topic, message.eventId(), message.attemptId());
    }
}
