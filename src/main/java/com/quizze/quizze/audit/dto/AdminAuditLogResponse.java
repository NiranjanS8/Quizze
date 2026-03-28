package com.quizze.quizze.audit.dto;

import com.quizze.quizze.audit.domain.AuditActionType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminAuditLogResponse {

    private Long id;
    private Long adminUserId;
    private String adminUsername;
    private AuditActionType actionType;
    private String targetType;
    private Long targetId;
    private String targetName;
    private String description;
    private LocalDateTime createdAt;
}
