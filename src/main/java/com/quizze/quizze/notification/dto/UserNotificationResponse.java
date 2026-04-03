package com.quizze.quizze.notification.dto;

import com.quizze.quizze.notification.domain.UserNotificationType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserNotificationResponse {

    private Long id;
    private UserNotificationType type;
    private String title;
    private String message;
    private boolean read;
    private Long relatedQuizId;
    private Long relatedAttemptId;
    private LocalDateTime createdAt;
}
