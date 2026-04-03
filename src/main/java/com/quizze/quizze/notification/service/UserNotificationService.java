package com.quizze.quizze.notification.service;

import com.quizze.quizze.notification.domain.UserNotification;
import com.quizze.quizze.notification.domain.UserNotificationType;
import com.quizze.quizze.notification.dto.UserNotificationResponse;
import com.quizze.quizze.notification.repository.UserNotificationRepository;
import com.quizze.quizze.user.domain.User;
import com.quizze.quizze.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserNotificationService {

    private final UserRepository userRepository;
    private final UserNotificationRepository userNotificationRepository;

    @Transactional
    public void createForUser(Long userId, UserNotificationType type, String title, String message, Long relatedQuizId, Long relatedAttemptId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }

        UserNotification notification = new UserNotification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRelatedQuizId(relatedQuizId);
        notification.setRelatedAttemptId(relatedAttemptId);
        userNotificationRepository.save(notification);
    }

    @Transactional
    public void createForOptedInUsers(UserNotificationType type, String title, String message, Long relatedQuizId) {
        int page = 0;
        while (true) {
            List<User> users = userRepository.findByEnabledTrueAndNewQuizNotificationsEnabledTrueOrderByIdAsc(
                    PageRequest.of(page, 100)
            );
            if (users.isEmpty()) {
                return;
            }

            List<UserNotification> notifications = users.stream().map(user -> {
                UserNotification notification = new UserNotification();
                notification.setUser(user);
                notification.setType(type);
                notification.setTitle(title);
                notification.setMessage(message);
                notification.setRelatedQuizId(relatedQuizId);
                return notification;
            }).toList();

            userNotificationRepository.saveAll(notifications);
            page++;
        }
    }

    @Transactional(readOnly = true)
    public List<UserNotificationResponse> getNotifications(Long userId) {
        return userNotificationRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(notification -> UserNotificationResponse.builder()
                        .id(notification.getId())
                        .type(notification.getType())
                        .title(notification.getTitle())
                        .message(notification.getMessage())
                        .read(notification.isRead())
                        .relatedQuizId(notification.getRelatedQuizId())
                        .relatedAttemptId(notification.getRelatedAttemptId())
                        .createdAt(notification.getCreatedAt())
                        .build())
                .toList();
    }
}
