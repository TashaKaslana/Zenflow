package org.phong.zenflow.workflow.infrastructure.mapstruct;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.phong.zenflow.workflow.dto.CreateWorkflowRequest;
import org.phong.zenflow.workflow.dto.UpdateWorkflowRequest;
import org.phong.zenflow.workflow.dto.WorkflowDto;
import org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface WorkflowMapper {
    @Mapping(source = "project.id", target = "projectId")
    WorkflowDto toDto(Workflow workflow);

    @Mapping(source = "projectId", target = "project.id")
    Workflow toEntity(CreateWorkflowRequest createWorkflowRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Workflow partialUpdate(UpdateWorkflowRequest updateWorkflowRequest, @MappingTarget Workflow workflow);
}