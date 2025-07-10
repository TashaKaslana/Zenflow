package org.phong.zenflow.log.systemlog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.log.systemlog.dto.SystemLogDto;
import org.phong.zenflow.log.systemlog.enums.SystemLogType;
import org.phong.zenflow.log.systemlog.infrastructure.mapper.SystemLogMapper;
import org.phong.zenflow.log.systemlog.infrastructure.persistence.entity.SystemLog;
import org.phong.zenflow.log.systemlog.infrastructure.persistence.repository.SystemLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemLogService {
    private final SystemLogRepository systemLogRepository;
    private final SystemLogMapper systemLogMapper;

    public void logInfo(String message, Object context) {
        persistLog(SystemLogType.INFO, message, context);
    }

    public void logError(String message, Object context) {
        persistLog(SystemLogType.ERROR, message, context);
    }

    public void logStartup(String message) {
        persistLog(SystemLogType.STARTUP, message, null);
    }

    public void logPluginEvent(String message, Object context) {
        persistLog(SystemLogType.PLUGIN, message, context);
    }

    public void persistLog(SystemLogType type, String message, Object context) {
        Map<String, Object> map = ObjectConversion.convertObjectToMap(context);
        persistLog(type, message, map);
    }

    private void persistLog(SystemLogType type, String message, Map<String, Object> context) {
        SystemLog logEntity = new SystemLog();
        logEntity.setLogType(type);
        logEntity.setMessage(message);
        logEntity.setContext(context);
        logEntity.setCreatedAt(OffsetDateTime.now());
        systemLogRepository.save(logEntity);
    }

    public Page<SystemLogDto> getLogsByType(Pageable pageable, SystemLogType type) {
        return systemLogRepository.findAllByLogType(type, pageable).map(systemLogMapper::toDto);
    }
}