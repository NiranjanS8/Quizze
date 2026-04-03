package com.quizze.quizze.notification.repository;

import com.quizze.quizze.notification.domain.ProcessedKafkaEvent;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedKafkaEventRepository extends JpaRepository<ProcessedKafkaEvent, Long> {

    Optional<ProcessedKafkaEvent> findByEventIdAndConsumerName(String eventId, String consumerName);
}
