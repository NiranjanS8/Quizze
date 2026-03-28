package com.quizze.quizze.audit.service;

import com.quizze.quizze.audit.domain.AdminAuditLog;
import com.quizze.quizze.audit.domain.AuditActionType;
import com.quizze.quizze.audit.dto.AdminAuditLogResponse;
import com.quizze.quizze.audit.mapper.AdminAuditLogMapper;
import com.quizze.quizze.audit.repository.AdminAuditLogRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminAuditLogService {

    private final AdminAuditLogRepository adminAuditLogRepository;
    private final AdminAuditLogMapper adminAuditLogMapper;

    @Transactional
    public void recordAction(
            Long adminUserId,
            String adminUsername,
            AuditActionType actionType,
            String targetType,
            Long targetId,
            String targetName,
            String description
    ) {
        AdminAuditLog auditLog = new AdminAuditLog();
        auditLog.setAdminUserId(adminUserId);
        auditLog.setAdminUsername(adminUsername);
        auditLog.setActionType(actionType);
        auditLog.setTargetType(targetType);
        auditLog.setTargetId(targetId);
        auditLog.setTargetName(targetName);
        auditLog.setDescription(description);
        adminAuditLogRepository.save(auditLog);

        log.info(
                "Admin audit log recorded: adminUserId={}, actionType={}, targetType={}, targetId={}",
                adminUserId, actionType, targetType, targetId
        );
    }

    @Transactional(readOnly = true)
    public List<AdminAuditLogResponse> getRecentLogs(int limit) {
        return adminAuditLogRepository.findAll().stream()
                .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
                .limit(Math.min(Math.max(limit, 1), 100))
                .map(adminAuditLogMapper::toResponse)
                .toList();
    }
}
