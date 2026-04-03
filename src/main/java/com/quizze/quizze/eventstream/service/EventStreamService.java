package com.quizze.quizze.eventstream.service;

import com.quizze.quizze.eventstream.domain.EventStreamEntry;
import com.quizze.quizze.eventstream.repository.EventStreamEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventStreamService {

    private final EventStreamEntryRepository eventStreamEntryRepository;

    @Transactional
    public void append(String eventType, String aggregateType, Long aggregateId, Long actorUserId, String summary) {
        EventStreamEntry entry = new EventStreamEntry();
        entry.setEventType(eventType);
        entry.setAggregateType(aggregateType);
        entry.setAggregateId(aggregateId);
        entry.setActorUserId(actorUserId);
        entry.setSummary(summary);
        eventStreamEntryRepository.save(entry);
        log.info("Event stream entry appended: eventType={}, aggregateType={}, aggregateId={}", eventType, aggregateType, aggregateId);
    }
}
