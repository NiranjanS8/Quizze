package com.quizze.quizze.notification.listener;

import com.quizze.quizze.notification.kafka.QuizSubmittedMessage;
import com.quizze.quizze.notification.service.QuizResultEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notifications.quiz-result", name = "enabled", havingValue = "true")
public class QuizResultKafkaConsumer {

    private final QuizResultEmailService quizResultEmailService;

    @KafkaListener(
            topics = "${app.notifications.quiz-result.topic}",
            groupId = "${app.notifications.quiz-result.consumer-group-id}"
    )
    public void consume(@Payload QuizSubmittedMessage message) {
        try {
            log.info("Consuming quiz submitted message for attemptId={}", message.attemptId());
            quizResultEmailService.sendResultSummary(message);
        } catch (Exception ex) {
            log.warn(
                    "Quiz result consumer failed for attemptId={}. Message will not crash the consumer. Reason: {}",
                    message.attemptId(),
                    ex.getMessage()
            );
        }
    }
}
