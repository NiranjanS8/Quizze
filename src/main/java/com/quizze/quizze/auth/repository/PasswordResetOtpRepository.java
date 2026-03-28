package com.quizze.quizze.auth.repository;

import com.quizze.quizze.auth.domain.PasswordResetOtp;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {

    Optional<PasswordResetOtp> findTopByEmailOrderByCreatedAtDesc(String email);

    List<PasswordResetOtp> findByEmailAndUsedAtIsNull(String email);
}
