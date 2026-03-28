package com.quizze.quizze.audit.domain;

import com.quizze.quizze.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "admin_audit_logs")
public class AdminAuditLog extends BaseEntity {

    @Column(nullable = false)
    private Long adminUserId;

    @Column(nullable = false, length = 50)
    private String adminUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AuditActionType actionType;

    @Column(nullable = false, length = 30)
    private String targetType;

    @Column(nullable = false)
    private Long targetId;

    @Column(nullable = false, length = 255)
    private String targetName;

    @Column(nullable = false, length = 1000)
    private String description;
}
