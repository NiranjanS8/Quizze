package com.quizze.quizze.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "processed_kafka_events")
@Getter
@Setter
@NoArgsConstructor
public class ProcessedKafkaEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String eventId;

    @Column(nullable = false, length = 80)
    private String eventType;

    @Column(nullable = false, length = 120)
    private String consumerName;

    @Column(nullable = false)
    private LocalDateTime processedAt;

    public ProcessedKafkaEvent(String eventId, String eventType, String consumerName, LocalDateTime processedAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.consumerName = consumerName;
        this.processedAt = processedAt;
    }
}
