package com.quizze.quizze.eventstream.repository;

import com.quizze.quizze.eventstream.domain.EventStreamEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventStreamEntryRepository extends JpaRepository<EventStreamEntry, Long> {
}
