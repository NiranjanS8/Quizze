package com.quizze.quizze.config;

import com.quizze.quizze.notification.config.MailProperties;
import com.quizze.quizze.notification.config.NewQuizNotificationProperties;
import com.quizze.quizze.notification.config.QuizResultNotificationProperties;
import com.quizze.quizze.security.jwt.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        JwtProperties.class,
        MailProperties.class,
        NewQuizNotificationProperties.class,
        QuizResultNotificationProperties.class
})
public class ApplicationConfig {
}
