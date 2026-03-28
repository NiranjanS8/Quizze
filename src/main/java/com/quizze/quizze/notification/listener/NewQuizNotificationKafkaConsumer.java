package com.quizze.quizze.notification.listener;

import com.quizze.quizze.notification.kafka.NewQuizPublishedMessage;
import com.quizze.quizze.notification.service.NewQuizNotificationEmailService;
import com.quizze.quizze.monitoring.service.ApplicationMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notifications.new-quiz", name = "enabled", havingValue = "true")
public class NewQuizNotificationKafkaConsumer {

    private final NewQuizNotificationEmailService newQuizNotificationEmailService;
    private final ApplicationMetricsService metricsService;

    @KafkaListener(
            topics = "${app.notifications.new-quiz.topic}",
            groupId = "${app.notifications.new-quiz.consumer-group-id}"
    )
    public void consume(@Payload NewQuizPublishedMessage message) {
        try {
            metricsService.increment("quizze.kafka.new_quiz.consume.success");
            log.info("Consuming new quiz notification event for quizId={}", message.quizId());
            newQuizNotificationEmailService.notifyOptedInUsers(message);
        } catch (Exception ex) {
            metricsService.increment("quizze.kafka.new_quiz.consume.failed");
            log.warn(
                    "New quiz notification consumer failed for quizId={}. Message will not crash the consumer. Reason: {}",
                    message.quizId(),
                    ex.getMessage()
            );
        }
    }
}
