package com.quizze.quizze.quiz.listener;

import com.quizze.quizze.cache.service.QuizCacheInvalidationService;
import com.quizze.quizze.quiz.event.QuizSubmittedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class QuizSubmittedEventListener {

    private final QuizCacheInvalidationService quizCacheInvalidationService;

    @EventListener
    public void handleQuizSubmitted(QuizSubmittedEvent event) {
        log.debug(
                "Handling QuizSubmittedEvent for attemptId={}, quizId={}, userId={}",
                event.attemptId(),
                event.quizId(),
                event.userId()
        );
        quizCacheInvalidationService.evictAfterQuizSubmission(event.quizId(), event.userId());
    }
}
