package org.phong.zenflow.log.auditlog.listeners;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.services.AuthService;
import org.phong.zenflow.core.utils.HttpRequestUtils;
import org.phong.zenflow.log.auditlog.dtos.CreateAuditLog;
import org.phong.zenflow.log.auditlog.events.AuditLogPublishableEvent;
import org.phong.zenflow.log.auditlog.events.CreateAuditLogPayload;
import org.phong.zenflow.log.auditlog.service.AuditLogService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@AllArgsConstructor
@Component
@Slf4j
public class AuditLogListener {
    private final AuditLogService activityLoggingService;
    private final AuthService authService;

    @TransactionalEventListener
    @Async("applicationTaskExecutor")
    public void handleHistoryActivityEvent(AuditLogPublishableEvent event) {
        CreateAuditLogPayload auditLog = event.getAuditLog();

        HttpServletRequest request = HttpRequestUtils.getCurrentHttpRequest();
        String ipAddress = null;
        String userAgent = null;
        if (request != null) {
            ipAddress = HttpRequestUtils.getClientIpAddress(request);
            userAgent = request.getHeader("User-Agent");
        } else {
            log.warn("AOP: HttpServletRequest not found for a method. IP/UA will be null.");
        }


        activityLoggingService.logActivity(new CreateAuditLog(
                authService.getUserIdFromContext(),
                auditLog.action().getAction(),
                auditLog.action().getTargetType().getValue(),
                auditLog.targetId(),
                auditLog.description(),
                auditLog.metadata(),
                ipAddress,
                userAgent
        ));
    }
}
