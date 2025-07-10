package org.phong.zenflow.log.systemlog.infrastructure.persistence.repository;

import org.phong.zenflow.log.systemlog.enums.SystemLogType;
import org.phong.zenflow.log.systemlog.infrastructure.persistence.entity.SystemLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SystemLogRepository extends JpaRepository<SystemLog, UUID> {
  Page<SystemLog> findAllByLogType(SystemLogType logType, Pageable pageable);
}