package com.quizze.quizze.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.refresh-token")
public class RefreshTokenProperties {

    private long expirationMs = 604800000L;
}
