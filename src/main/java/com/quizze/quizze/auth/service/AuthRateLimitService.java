package com.quizze.quizze.auth.service;

import com.quizze.quizze.auth.config.AuthRateLimitProperties;
import com.quizze.quizze.common.exception.TooManyRequestsException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthRateLimitService {

    private final AuthRateLimitProperties properties;
    private final ConcurrentMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public void checkLoginLimit(String clientIp, String usernameOrEmail) {
        if (!properties.isEnabled()) {
            return;
        }

        String normalizedPrincipal = normalizeValue(usernameOrEmail);
        consume("login:" + normalizeValue(clientIp) + ":" + normalizedPrincipal,
                properties.getLoginMaxAttempts(),
                properties.getLoginWindowSeconds(),
                "Too many login attempts. Please wait and try again.");
    }

    public void checkForgotPasswordLimit(String clientIp, String email) {
        if (!properties.isEnabled()) {
            return;
        }

        consume("forgot-password:" + normalizeValue(clientIp) + ":" + normalizeValue(email),
                properties.getForgotPasswordMaxAttempts(),
                properties.getForgotPasswordWindowSeconds(),
                "Too many OTP requests. Please wait before requesting another code.");
    }

    public void checkResetPasswordLimit(String clientIp, String email) {
        if (!properties.isEnabled()) {
            return;
        }

        consume("reset-password:" + normalizeValue(clientIp) + ":" + normalizeValue(email),
                properties.getResetPasswordMaxAttempts(),
                properties.getResetPasswordWindowSeconds(),
                "Too many password reset attempts. Please wait before trying again.");
    }

    private void consume(String key, int maxAttempts, long windowSeconds, String message) {
        Instant now = Instant.now();
        WindowCounter counter = counters.computeIfAbsent(key, ignored -> new WindowCounter(now, 0));

        synchronized (counter) {
            if (Duration.between(counter.windowStart(), now).getSeconds() >= windowSeconds) {
                counter.reset(now);
            }

            if (counter.attempts() >= maxAttempts) {
                throw new TooManyRequestsException(message);
            }

            counter.increment();
        }
    }

    private String normalizeValue(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static final class WindowCounter {
        private Instant windowStart;
        private int attempts;

        private WindowCounter(Instant windowStart, int attempts) {
            this.windowStart = windowStart;
            this.attempts = attempts;
        }

        private Instant windowStart() {
            return windowStart;
        }

        private int attempts() {
            return attempts;
        }

        private void increment() {
            attempts++;
        }

        private void reset(Instant now) {
            this.windowStart = now;
            this.attempts = 0;
        }
    }
}
