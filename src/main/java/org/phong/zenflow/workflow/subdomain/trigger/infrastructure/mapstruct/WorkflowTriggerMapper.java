package org.phong.zenflow.workflow.subdomain.trigger.infrastructure.mapstruct;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.phong.zenflow.workflow.subdomain.trigger.dto.CreateWorkflowTriggerRequest;
import org.phong.zenflow.workflow.subdomain.trigger.dto.UpdateWorkflowTriggerRequest;
import org.phong.zenflow.workflow.subdomain.trigger.dto.WorkflowTriggerDto;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface WorkflowTriggerMapper {
    WorkflowTriggerDto toDto(WorkflowTrigger entity);

    WorkflowTrigger toEntity(CreateWorkflowTriggerRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(UpdateWorkflowTriggerRequest request, @MappingTarget WorkflowTrigger entity);
}
