package com.quizze.quizze.notification.repository;

import com.quizze.quizze.notification.domain.UserNotification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    List<UserNotification> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndReadFalse(Long userId);

    Optional<UserNotification> findByIdAndUserId(Long id, Long userId);

    List<UserNotification> findByUserIdAndReadFalse(Long userId);
}
