package com.quizze.quizze.notification.listener;

import com.quizze.quizze.audit.event.AdminActionEvent;
import com.quizze.quizze.audit.service.AdminAuditLogService;
import com.quizze.quizze.eventstream.service.EventStreamService;
import com.quizze.quizze.notification.domain.UserNotificationType;
import com.quizze.quizze.notification.event.QuizPublishedEvent;
import com.quizze.quizze.notification.service.UserNotificationService;
import com.quizze.quizze.quiz.event.QuizSubmittedEvent;
import com.quizze.quizze.quiz.service.QuizAnalyticsProjectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class AsyncWorkflowListener {

    private final EventStreamService eventStreamService;
    private final UserNotificationService userNotificationService;
    private final QuizAnalyticsProjectionService quizAnalyticsProjectionService;
    private final AdminAuditLogService adminAuditLogService;

    @Async("applicationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleQuizPublished(QuizPublishedEvent event) {
        log.info("Async handling QuizPublishedEvent for quizId={}", event.quizId());
        eventStreamService.append(
                "QUIZ_PUBLISHED",
                "QUIZ",
                event.quizId(),
                null,
                "Quiz '" + event.quizTitle() + "' was published"
        );
        userNotificationService.createForOptedInUsers(
                UserNotificationType.QUIZ_PUBLISHED,
                "New quiz available",
                "A new quiz, '" + event.quizTitle() + "', is now available.",
                event.quizId()
        );
    }

    @Async("applicationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleQuizSubmitted(QuizSubmittedEvent event) {
        log.info("Async handling QuizSubmittedEvent for attemptId={}", event.attemptId());
        eventStreamService.append(
                "QUIZ_SUBMITTED",
                "QUIZ_ATTEMPT",
                event.attemptId(),
                event.userId(),
                "Quiz submission completed for attemptId=" + event.attemptId()
        );
        quizAnalyticsProjectionService.rebuildForQuiz(event.quizId());
        userNotificationService.createForUser(
                event.userId(),
                UserNotificationType.QUIZ_RESULT_READY,
                "Quiz result ready",
                "Your quiz result is ready to review.",
                event.quizId(),
                event.attemptId()
        );
    }

    @Async("applicationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAdminAction(AdminActionEvent event) {
        log.info("Async handling AdminActionEvent actionType={} targetType={} targetId={}",
                event.actionType(), event.targetType(), event.targetId());
        adminAuditLogService.recordAction(
                event.adminUserId(),
                event.adminUsername(),
                event.actionType(),
                event.targetType(),
                event.targetId(),
                event.targetName(),
                event.description()
        );
        eventStreamService.append(
                event.actionType().name(),
                event.targetType(),
                event.targetId(),
                event.adminUserId(),
                event.description()
        );
    }
}
