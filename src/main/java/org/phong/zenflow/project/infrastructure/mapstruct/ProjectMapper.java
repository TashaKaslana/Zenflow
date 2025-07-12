package org.phong.zenflow.project.infrastructure.mapstruct;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.phong.zenflow.project.dto.CreateProjectRequest;
import org.phong.zenflow.project.dto.ProjectDto;
import org.phong.zenflow.project.dto.UpdateProjectRequest;
import org.phong.zenflow.project.infrastructure.persistence.entity.Project;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProjectMapper {
    @Mapping(source = "user.id", target = "userId")
    ProjectDto toDto(Project project);

    Project toEntity(CreateProjectRequest createProjectRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Project partialUpdate(UpdateProjectRequest updateProjectRequest, @MappingTarget Project project);
}