package com.quizze.quizze.audit.repository;

import com.quizze.quizze.audit.domain.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
}
