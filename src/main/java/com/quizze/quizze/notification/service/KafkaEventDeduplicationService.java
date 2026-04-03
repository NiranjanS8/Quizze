package com.quizze.quizze.notification.service;

import com.quizze.quizze.notification.domain.ProcessedKafkaEvent;
import com.quizze.quizze.notification.repository.ProcessedKafkaEventRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaEventDeduplicationService {

    private final ProcessedKafkaEventRepository processedKafkaEventRepository;

    public boolean isAlreadyProcessed(String eventId, String consumerName) {
        return processedKafkaEventRepository.findByEventIdAndConsumerName(eventId, consumerName).isPresent();
    }

    public void markProcessed(String eventId, String eventType, String consumerName) {
        if (isAlreadyProcessed(eventId, consumerName)) {
            return;
        }

        processedKafkaEventRepository.save(
                new ProcessedKafkaEvent(eventId, eventType, consumerName, LocalDateTime.now())
        );
    }
}
