package org.phong.zenflow.plugin.infrastructure.mapstruct;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.phong.zenflow.plugin.dto.CreatePluginRequest;
import org.phong.zenflow.plugin.dto.PluginDto;
import org.phong.zenflow.plugin.dto.UpdatePluginRequest;
import org.phong.zenflow.plugin.infrastructure.persistence.entity.Plugin;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface PluginMapper {
    PluginDto toDto(Plugin plugin);

    Plugin toEntity(CreatePluginRequest createPluginRequest);

    Plugin toEntity(UpdatePluginRequest updatePluginRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Plugin partialUpdate(UpdatePluginRequest updatePluginRequest, @MappingTarget Plugin plugin);
}