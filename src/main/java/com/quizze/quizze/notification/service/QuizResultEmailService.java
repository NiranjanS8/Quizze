package com.quizze.quizze.notification.service;

import com.quizze.quizze.notification.config.MailProperties;
import com.quizze.quizze.notification.config.QuizResultNotificationProperties;
import com.quizze.quizze.notification.kafka.QuizSubmittedMessage;
import com.quizze.quizze.quiz.domain.QuizAttempt;
import com.quizze.quizze.quiz.repository.QuizAttemptRepository;
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
public class QuizResultEmailService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final QuizResultNotificationProperties properties;
    private final QuizAttemptRepository quizAttemptRepository;

    public void sendResultSummary(QuizSubmittedMessage message) {
        if (!mailProperties.isEnabled()) {
            log.debug("Quiz result email skipped because app.mail.enabled=false for attemptId={}", message.attemptId());
            return;
        }

        QuizAttempt attempt = quizAttemptRepository.findById(message.attemptId()).orElse(null);
        if (attempt == null || attempt.getUser() == null) {
            log.warn("Quiz result email skipped because attemptId={} could not be loaded", message.attemptId());
            return;
        }

        int maxAttempts = Math.max(1, properties.getMaxRetryAttempts());
        for (int attemptNumber = 1; attemptNumber <= maxAttempts; attemptNumber++) {
            try {
                mailSender.send(buildMessage(attempt));
                log.info("Quiz result email sent for attemptId={} on attempt {}", message.attemptId(), attemptNumber);
                return;
            } catch (MailException | MessagingException | UnsupportedEncodingException ex) {
                if (attemptNumber == maxAttempts) {
                    log.warn(
                            "Quiz result email failed for attemptId={} after {} attempts. Reason: {}",
                            message.attemptId(),
                            maxAttempts,
                            ex.getMessage()
                    );
                } else {
                    log.warn(
                            "Retrying quiz result email for attemptId={}. Attempt {}/{}. Reason: {}",
                            message.attemptId(),
                            attemptNumber,
                            maxAttempts,
                            ex.getMessage()
                    );
                }
            }
        }
    }

    private MimeMessage buildMessage(QuizAttempt attempt) throws MessagingException, UnsupportedEncodingException {
        double maxScore = attempt.getQuiz().getQuestions().stream().mapToInt(question -> question.getPoints()).sum();
        double percentage = maxScore == 0.0 ? 0.0 : Math.max(0.0, (attempt.getScore() / maxScore) * 100.0);

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());
        helper.setTo(attempt.getUser().getEmail());
        helper.setFrom(mailProperties.getFromAddress(), mailProperties.getFromName());
        helper.setSubject("Your Quizze result for " + attempt.getQuiz().getTitle());
        helper.setText(buildEmailBody(attempt, maxScore, percentage), false);
        return mimeMessage;
    }

    private String buildEmailBody(QuizAttempt attempt, double maxScore, double percentage) {
        return "Hi " + attempt.getUser().getFirstName() + ",\n\n"
                + "Your quiz result is ready.\n\n"
                + "Quiz: " + attempt.getQuiz().getTitle() + "\n"
                + "Score: " + attempt.getScore() + " / " + maxScore + "\n"
                + "Percentage: " + Math.round(percentage) + "%\n"
                + "Correct Answers: " + attempt.getCorrectAnswers() + "\n"
                + "Wrong Answers: " + attempt.getWrongAnswers() + "\n\n"
                + "Log in to Quizze to review your detailed answers.\n\n"
                + "Thanks,\n"
                + mailProperties.getFromName();
    }
}
