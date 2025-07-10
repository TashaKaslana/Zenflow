package org.phong.zenflow.log.systemlog.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.phong.zenflow.log.systemlog.dto.SystemLogDto;
import org.phong.zenflow.log.systemlog.infrastructure.persistence.entity.SystemLog;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface SystemLogMapper {
    SystemLogDto toDto(SystemLog systemLog);
}