package com.quizze.quizze.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.rate-limit.auth")
public class AuthRateLimitProperties {

    private boolean enabled = true;
    private int loginMaxAttempts = 10;
    private long loginWindowSeconds = 60;
    private int forgotPasswordMaxAttempts = 5;
    private long forgotPasswordWindowSeconds = 300;
    private int resetPasswordMaxAttempts = 5;
    private long resetPasswordWindowSeconds = 300;
}
