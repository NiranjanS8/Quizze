package com.quizze.quizze.notification.service;

import com.quizze.quizze.notification.config.MailProperties;
import com.quizze.quizze.user.domain.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
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
public class PasswordResetEmailService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    public void sendOtp(User user, String otp) {
        if (!mailProperties.isEnabled()) {
            log.debug("Password reset email skipped because app.mail.enabled=false for userId={}", user.getId());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setTo(user.getEmail());
            helper.setFrom(mailProperties.getFromAddress(), mailProperties.getFromName());
            helper.setSubject("Quizze password reset OTP");
            helper.setText(buildMessage(user, otp), false);
            mailSender.send(message);
            log.info("Password reset OTP sent successfully to userId={} at email='{}'", user.getId(), user.getEmail());
        } catch (MailException | MessagingException | UnsupportedEncodingException ex) {
            log.warn(
                    "Password reset OTP email could not be sent for userId={} at email='{}'. Reason: {}",
                    user.getId(), user.getEmail(), ex.getMessage()
            );
        }
    }

    private String buildMessage(User user, String otp) {
        return "Hi " + user.getFirstName() + ",\n\n"
                + "We received a request to reset your Quizze password.\n\n"
                + "Your OTP is: " + otp + "\n"
                + "This code will expire in 10 minutes.\n\n"
                + "If you did not request this, you can ignore this email.\n\n"
                + "Thanks,\n"
                + mailProperties.getFromName();
    }
}
