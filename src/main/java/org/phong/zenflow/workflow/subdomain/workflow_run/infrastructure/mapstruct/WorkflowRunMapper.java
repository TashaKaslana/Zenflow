package org.phong.zenflow.workflow.subdomain.workflow_run.infrastructure.mapstruct;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.phong.zenflow.workflow.subdomain.workflow_run.dto.CreateWorkflowRunRequest;
import org.phong.zenflow.workflow.subdomain.workflow_run.dto.UpdateWorkflowRunRequest;
import org.phong.zenflow.workflow.subdomain.workflow_run.dto.WorkflowRunDto;
import org.phong.zenflow.workflow.subdomain.workflow_run.infrastructure.persistence.entity.WorkflowRun;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface WorkflowRunMapper {
    @Mapping(source = "workflow.id", target = "workflowId")
    WorkflowRunDto toDto(WorkflowRun workflowRun);

    @Mapping(source = "workflowId", target = "workflow.id")
    WorkflowRun toEntity(CreateWorkflowRunRequest createWorkflowRunRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    WorkflowRun partialUpdate(UpdateWorkflowRunRequest updateWorkflowRunRequest, @MappingTarget WorkflowRun workflowRun);
}
