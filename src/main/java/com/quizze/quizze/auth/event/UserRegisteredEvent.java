package com.quizze.quizze.auth.event;

import com.quizze.quizze.user.domain.User;

public record UserRegisteredEvent(User user) {
}
