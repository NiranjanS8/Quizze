package com.quizze.quizze.auth.listener;

import com.quizze.quizze.auth.event.UserRegisteredEvent;
import com.quizze.quizze.notification.service.WelcomeEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserRegisteredEventListener {

    private final WelcomeEmailService welcomeEmailService;

    @EventListener
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.debug("Handling UserRegisteredEvent for userId={}", event.user().getId());
        welcomeEmailService.sendWelcomeEmail(event.user());
    }
}
