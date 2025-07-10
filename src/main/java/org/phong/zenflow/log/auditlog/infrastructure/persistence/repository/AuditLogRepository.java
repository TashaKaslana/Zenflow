package org.phong.zenflow.log.auditlog.infrastructure.persistence.repository;

import org.phong.zenflow.log.auditlog.infrastructure.persistence.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {
}