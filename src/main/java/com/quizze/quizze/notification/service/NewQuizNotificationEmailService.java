package com.quizze.quizze.notification.service;

import com.quizze.quizze.notification.config.MailProperties;
import com.quizze.quizze.notification.config.NewQuizNotificationProperties;
import com.quizze.quizze.notification.kafka.NewQuizPublishedMessage;
import com.quizze.quizze.user.domain.User;
import com.quizze.quizze.user.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NewQuizNotificationEmailService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final NewQuizNotificationProperties properties;
    private final UserRepository userRepository;

    public void notifyOptedInUsers(NewQuizPublishedMessage message) {
        if (!mailProperties.isEnabled()) {
            log.debug("New quiz notification emails skipped because app.mail.enabled=false for quizId={}", message.quizId());
            return;
        }

        int page = 0;
        while (true) {
            List<User> batch = userRepository.findByEnabledTrueAndNewQuizNotificationsEnabledTrueOrderByIdAsc(
                    PageRequest.of(page, properties.getBatchSize())
            );

            if (batch.isEmpty()) {
                return;
            }

            sendBatch(batch, message);
            page++;
        }
    }

    private void sendBatch(List<User> users, NewQuizPublishedMessage message) {
        try {
            List<MimeMessage> messages = new ArrayList<>();
            for (User user : users) {
                messages.add(buildMessage(user, message));
            }
            mailSender.send(messages.toArray(MimeMessage[]::new));
            log.info("Sent new quiz notification batch for quizId={} to {} opted-in users", message.quizId(), users.size());
        } catch (MailException | MessagingException | UnsupportedEncodingException ex) {
            log.warn(
                    "Batch send failed for new quiz notification quizId={}. Falling back to per-user send. Reason: {}",
                    message.quizId(),
                    ex.getMessage()
            );
            users.forEach(user -> sendWithRetry(user, message));
        }
    }

    private void sendWithRetry(User user, NewQuizPublishedMessage message) {
        int maxAttempts = Math.max(1, properties.getMaxRetryAttempts());
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                mailSender.send(buildMessage(user, message));
                log.info(
                        "Sent new quiz notification for quizId={} to userId={} on attempt {}",
                        message.quizId(),
                        user.getId(),
                        attempt
                );
                return;
            } catch (MailException | MessagingException | UnsupportedEncodingException ex) {
                if (attempt == maxAttempts) {
                    log.warn(
                            "New quiz notification failed for userId={} and quizId={} after {} attempts. Reason: {}",
                            user.getId(),
                            message.quizId(),
                            maxAttempts,
                            ex.getMessage()
                    );
                } else {
                    log.warn(
                            "Retrying new quiz notification for userId={} and quizId={}. Attempt {}/{}. Reason: {}",
                            user.getId(),
                            message.quizId(),
                            attempt,
                            maxAttempts,
                            ex.getMessage()
                    );
                }
            }
        }
    }

    private MimeMessage buildMessage(User user, NewQuizPublishedMessage message)
            throws MessagingException, UnsupportedEncodingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());
        helper.setTo(user.getEmail());
        helper.setFrom(mailProperties.getFromAddress(), mailProperties.getFromName());
        helper.setSubject("New quiz available: " + message.quizTitle());
        helper.setText(buildEmailBody(user, message), false);
        return mimeMessage;
    }

    private String buildEmailBody(User user, NewQuizPublishedMessage message) {
        String category = message.categoryName() == null || message.categoryName().isBlank()
                ? "General"
                : message.categoryName();

        String description = message.quizDescription() == null || message.quizDescription().isBlank()
                ? "A new quiz has been published and is ready to take."
                : message.quizDescription();

        return "Hi " + user.getFirstName() + ",\n\n"
                + "A new quiz has just been published on Quizze.\n\n"
                + "Title: " + message.quizTitle() + "\n"
                + "Category: " + category + "\n"
                + "Description: " + description + "\n\n"
                + "Log in to your account to start the quiz.\n\n"
                + "Thanks,\n"
                + mailProperties.getFromName();
    }
}
