package org.phong.zenflow.secret.infrastructure.mapstruct;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.phong.zenflow.secret.dto.CreateSecretRequest;
import org.phong.zenflow.secret.dto.SecretDto;
import org.phong.zenflow.secret.dto.UpdateSecretRequest;
import org.phong.zenflow.secret.infrastructure.persistence.entity.Secret;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface SecretMapper {
    @Mapping(target = "workflowId", source = "workflow.id")
    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "value", source = "encryptedValue")
    SecretDto toDto(Secret secret);

    @Mapping(target = "encryptedValue", source = "value")
    Secret toEntity(CreateSecretRequest createSecretRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "encryptedValue", source = "value")
    void updateEntityFromDto(UpdateSecretRequest updateSecretRequest, @MappingTarget Secret secret);
}