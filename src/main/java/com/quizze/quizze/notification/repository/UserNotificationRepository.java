package com.quizze.quizze.notification.repository;

import com.quizze.quizze.notification.domain.UserNotification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    List<UserNotification> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);
}
