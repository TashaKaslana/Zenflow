package org.phong.zenflow.log.systemlog.auto;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.log.systemlog.service.SystemLogService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ScheduledJobLogger {

    private final SystemLogService systemLogService;

    @Scheduled(fixedRate = 60000)
    public void runHeartbeatLogger() {
        systemLogService.logInfo("Heartbeat: scheduler is alive", null);
    }
}