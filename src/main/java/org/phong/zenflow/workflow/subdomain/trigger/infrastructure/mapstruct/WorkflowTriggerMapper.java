package org.phong.zenflow.workflow.subdomain.trigger.infrastructure.mapstruct;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.phong.zenflow.workflow.subdomain.trigger.dto.CreateWorkflowTriggerRequest;
import org.phong.zenflow.workflow.subdomain.trigger.dto.UpdateWorkflowTriggerRequest;
import org.phong.zenflow.workflow.subdomain.trigger.dto.WorkflowTriggerDto;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface WorkflowTriggerMapper {

    WorkflowTriggerDto toDto(WorkflowTrigger entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "lastTriggeredAt", ignore = true)
    WorkflowTrigger toEntity(CreateWorkflowTriggerRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "workflowId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "lastTriggeredAt", ignore = true)
    void updateEntity(UpdateWorkflowTriggerRequest request, @MappingTarget WorkflowTrigger entity);
}
