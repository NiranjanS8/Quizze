package com.quizze.quizze.audit.event;

import com.quizze.quizze.audit.domain.AuditActionType;

public record AdminActionEvent(
        Long adminUserId,
        String adminUsername,
        AuditActionType actionType,
        String targetType,
        Long targetId,
        String targetName,
        String description
) {
}
