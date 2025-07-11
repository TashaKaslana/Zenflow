package org.phong.zenflow.log.auditlog.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.log.auditlog.dtos.AuditLogDto;
import org.phong.zenflow.log.auditlog.dtos.CreateAuditLog;
import org.phong.zenflow.log.auditlog.infrastructure.mapstruct.AuditLogMapper;
import org.phong.zenflow.log.auditlog.infrastructure.persistence.entity.AuditLogEntity;
import org.phong.zenflow.log.auditlog.infrastructure.persistence.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;


    @Transactional
    public void logActivity(CreateAuditLog request) {
        logToConsole(request);

        try {
            AuditLogEntity auditLogEntity = auditLogMapper.toEntity(request);
            auditLogRepository.save(auditLogEntity);
        } catch (Exception e) {
            log.error("Failed to log: User: {}, Action: {}, Error Message: {}",
                    request.userId(), request.action(), e.getMessage()
            );
        }
    }

    public void logBulkActions(List<CreateAuditLog> actions) {
        List<AuditLogEntity> logs = actions.stream().map(auditLogMapper::toEntity).toList();

        auditLogRepository.saveAll(logs);
    }


    public Page<AuditLogDto> logPage(Pageable pageable) {
        Page<AuditLogEntity> auditLogEntities = auditLogRepository.findAll(pageable);
        return auditLogEntities.map(auditLogMapper::toDto);
    }

    private static void logToConsole(CreateAuditLog request) {
        //UA is user agent
        log.debug("ACTIVITY LOGGING : UserId {}, Action: {}, Desc: '{}', Target: [Type :{}, ID :{}], IP: {}, UA: {}, Detail: {}",
                request.userId(),
                request.action(),
                request.description(),
                request.targetType() != null ? request.targetType() : "N/A",
                request.targetId() != null ? request.targetId() : "N/A",
                request.ipAddress() != null ? request.ipAddress() : "N/A",
                request.userAgent() != null ? request.userAgent().substring(0, Math.min(request.userAgent().length(), 30)) + "..." : "N/A",
                request.metadata() != null ? request.metadata() : "N/A");
    }
}
