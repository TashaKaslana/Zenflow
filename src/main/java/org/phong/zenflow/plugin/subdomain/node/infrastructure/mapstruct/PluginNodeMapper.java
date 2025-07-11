package org.phong.zenflow.plugin.subdomain.node.infrastructure.mapstruct;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.phong.zenflow.plugin.subdomain.node.dto.CreatePluginNode;
import org.phong.zenflow.plugin.subdomain.node.dto.PluginNodeDto;
import org.phong.zenflow.plugin.subdomain.node.dto.UpdatePluginNodeRequest;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface PluginNodeMapper {
    @Mapping(source = "plugin.id", target = "pluginId")
    PluginNodeDto toDto(PluginNode pluginNode);

    @Mapping(source = "pluginId", target = "plugin.id")
    PluginNode toEntity(CreatePluginNode createPluginNode);

    PluginNode toEntity(UpdatePluginNodeRequest updatePluginNodeRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    PluginNode partialUpdate(UpdatePluginNodeRequest updatePluginNodeRequest, @MappingTarget PluginNode pluginNode);
}