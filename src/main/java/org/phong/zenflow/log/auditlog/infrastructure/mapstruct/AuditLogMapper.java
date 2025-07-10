package org.phong.zenflow.log.auditlog.infrastructure.mapstruct;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.phong.zenflow.log.auditlog.dtos.AuditLogDto;
import org.phong.zenflow.log.auditlog.dtos.CreateAuditLog;
import org.phong.zenflow.log.auditlog.infrastructure.persistence.entity.AuditLogEntity;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuditLogMapper {
    AuditLogDto toDto(AuditLogEntity auditLogEntity);

    AuditLogEntity toEntity(CreateAuditLog createAuditLog);
}