package com.quizze.quizze.notification.service;

import com.quizze.quizze.notification.config.MailProperties;
import com.quizze.quizze.user.domain.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class WelcomeEmailService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    public void sendWelcomeEmail(User user) {
        if (!mailProperties.isEnabled()) {
            log.debug("Welcome email skipped because app.mail.enabled=false for userId={}", user.getId());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setTo(user.getEmail());
            helper.setFrom(mailProperties.getFromAddress(), mailProperties.getFromName());
            helper.setSubject("Welcome to Quizze");
            helper.setText(buildWelcomeMessage(user), false);
            mailSender.send(message);
            log.info("Welcome email sent successfully to userId={} at email='{}'", user.getId(), user.getEmail());
        } catch (MailException | MessagingException | java.io.UnsupportedEncodingException ex) {
            log.warn(
                    "Welcome email could not be sent for userId={} at email='{}'. Registration will continue. Reason: {}",
                    user.getId(), user.getEmail(), ex.getMessage()
            );
        }
    }

    private String buildWelcomeMessage(User user) {
        return "Hi " + user.getFirstName() + ",\n\n"
                + "Welcome to Quizze.\n\n"
                + "Your account has been created successfully. You can now log in and start taking quizzes.\n\n"
                + "Username: " + user.getUsername() + "\n"
                + "Role: " + user.getRole().getName().name() + "\n\n"
                + "Thanks,\n"
                + mailProperties.getFromName();
    }
}
