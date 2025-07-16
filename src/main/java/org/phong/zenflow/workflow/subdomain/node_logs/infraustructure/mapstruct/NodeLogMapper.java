package org.phong.zenflow.workflow.subdomain.node_logs.infraustructure.mapstruct;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.phong.zenflow.workflow.subdomain.node_logs.dto.CreateNodeLogRequest;
import org.phong.zenflow.workflow.subdomain.node_logs.dto.NodeLogDto;
import org.phong.zenflow.workflow.subdomain.node_logs.dto.UpdateNodeLogRequest;
import org.phong.zenflow.workflow.subdomain.node_logs.infraustructure.persistence.entity.NodeLog;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface NodeLogMapper {
    @Mapping(source = "workflowRun.id", target = "workflowRunId")
    NodeLogDto toDto(NodeLog nodeLog);

    List<NodeLogDto> toDtoList(List<NodeLog> nodeLog);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    NodeLog partialUpdate(UpdateNodeLogRequest updateNodeLogRequest, @MappingTarget NodeLog nodeLog);

    @Mapping(source = "workflowRunId", target = "workflowRun.id")
    NodeLog toEntity(CreateNodeLogRequest createNodeLogRequest);
}