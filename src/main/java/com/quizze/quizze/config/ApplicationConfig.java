package com.quizze.quizze.config;

import com.quizze.quizze.auth.config.AuthRateLimitProperties;
import com.quizze.quizze.auth.config.RefreshTokenProperties;
import com.quizze.quizze.notification.config.MailProperties;
import com.quizze.quizze.notification.config.NewQuizNotificationProperties;
import com.quizze.quizze.notification.config.QuizResultNotificationProperties;
import com.quizze.quizze.security.jwt.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        AuthRateLimitProperties.class,
        RefreshTokenProperties.class,
        JwtProperties.class,
        MailProperties.class,
        NewQuizNotificationProperties.class,
        QuizResultNotificationProperties.class
})
public class ApplicationConfig {
}
