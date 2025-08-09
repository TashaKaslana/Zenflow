package org.phong.zenflow.workflow.subdomain.workflow_version.infrastructure.mapstruct;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.phong.zenflow.workflow.subdomain.workflow_version.dto.WorkflowVersionDto;
import org.phong.zenflow.workflow.subdomain.workflow_version.infrastructure.persistence.entity.WorkflowVersion;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface WorkflowVersionMapper {

    @Mapping(source = "workflow.id", target = "workflowId")
    WorkflowVersionDto toDto(WorkflowVersion workflowVersion);

    List<WorkflowVersionDto> toDtoList(List<WorkflowVersion> versions);
}

