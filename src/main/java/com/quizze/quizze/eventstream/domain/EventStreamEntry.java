package com.quizze.quizze.eventstream.domain;

import com.quizze.quizze.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "event_stream_entries")
public class EventStreamEntry extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String eventType;

    @Column(nullable = false, length = 80)
    private String aggregateType;

    @Column(nullable = false)
    private Long aggregateId;

    private Long actorUserId;

    @Column(length = 2000)
    private String summary;
}
