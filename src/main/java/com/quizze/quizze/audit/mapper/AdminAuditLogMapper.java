package com.quizze.quizze.audit.mapper;

import com.quizze.quizze.audit.domain.AdminAuditLog;
import com.quizze.quizze.audit.dto.AdminAuditLogResponse;
import org.springframework.stereotype.Component;

@Component
public class AdminAuditLogMapper {

    public AdminAuditLogResponse toResponse(AdminAuditLog auditLog) {
        return AdminAuditLogResponse.builder()
                .id(auditLog.getId())
                .adminUserId(auditLog.getAdminUserId())
                .adminUsername(auditLog.getAdminUsername())
                .actionType(auditLog.getActionType())
                .targetType(auditLog.getTargetType())
                .targetId(auditLog.getTargetId())
                .targetName(auditLog.getTargetName())
                .description(auditLog.getDescription())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}
