package org.phong.zenflow.workflow.subdomain.node_execution.infrastructure.mapstruct;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.phong.zenflow.workflow.subdomain.node_execution.dto.CreateNodeExecutionRequest;
import org.phong.zenflow.workflow.subdomain.node_execution.dto.NodeExecutionDto;
import org.phong.zenflow.workflow.subdomain.node_execution.dto.UpdateNodeExecutionRequest;
import org.phong.zenflow.workflow.subdomain.node_execution.infrastructure.persistence.entity.NodeExecution;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface NodeExecutionMapper {
    @Mapping(source = "workflowRun.id", target = "workflowRunId")
    NodeExecutionDto toDto(NodeExecution nodeExecution);

    List<NodeExecutionDto> toDtoList(List<NodeExecution> nodeExecutions);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    NodeExecution partialUpdate(UpdateNodeExecutionRequest updateNodeExecutionRequest, @MappingTarget NodeExecution nodeExecution);

    NodeExecution toEntity(CreateNodeExecutionRequest createNodeExecutionRequest);
}