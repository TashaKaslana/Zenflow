package org.phong.zenflow.workflow.subdomain.logging.api.infrastructure.mapstruct;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.phong.zenflow.workflow.subdomain.logging.api.dto.CreateNodeLogRequest;
import org.phong.zenflow.workflow.subdomain.logging.api.dto.NodeLogDto;
import org.phong.zenflow.workflow.subdomain.logging.api.dto.UpdateNodeLogRequest;
import org.phong.zenflow.workflow.subdomain.logging.api.infrastructure.persistence.entity.NodeLog;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface NodeLogMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "workflowRun", ignore = true)
    @Mapping(target = "timestamp", expression = "java(request.timestamp() != null ? request.timestamp() : java.time.OffsetDateTime.now())")
    NodeLog toEntity(CreateNodeLogRequest request);

    @Mapping(source = "workflowRun.id", target = "workflowRunId")
    NodeLogDto toDto(NodeLog entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(UpdateNodeLogRequest request, @MappingTarget NodeLog entity);
}
