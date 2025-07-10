package org.phong.zenflow.log.systemlog.auto;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.phong.zenflow.log.systemlog.service.SystemLogService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
class SystemStartupLogger {

    private final SystemLogService systemLogService;

    @PostConstruct
    public void onStartup() {
        systemLogService.logStartup("System started at: " + LocalDateTime.now());
    }
}