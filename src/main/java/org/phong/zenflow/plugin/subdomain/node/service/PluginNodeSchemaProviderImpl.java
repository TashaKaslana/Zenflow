package org.phong.zenflow.plugin.subdomain.node.service;

import org.phong.zenflow.plugin.subdomain.node.exception.ValidateNodeSchemaException;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.projections.PluginNodeSchema;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.repository.PluginNodeRepository;
import org.phong.zenflow.plugin.subdomain.node.interfaces.PluginNodeSchemaProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PluginNodeSchemaProviderImpl implements PluginNodeSchemaProvider {

    private final PluginNodeRepository pluginNodeRepository;

    public PluginNodeSchemaProviderImpl(PluginNodeRepository pluginNodeRepository) {
        this.pluginNodeRepository = pluginNodeRepository;
    }

    @Override
    public Map<String, Object> getSchemaJson(UUID nodeId) {
        return pluginNodeRepository
                .findByIdCustom(nodeId)
                .map(PluginNodeSchema::getConfigSchema).orElseThrow(
                        () -> new ValidateNodeSchemaException("Node schema not found for nodeId: " +  nodeId)
                );
    }

    @Override
    public Map<UUID, Map<String, Object>> getAllSchemasByNodeIds(List<UUID> nodeIds) {
        return pluginNodeRepository.findAllByIds(nodeIds)
                .stream()
                .filter(node -> node.getConfigSchema() != null)
                .collect(Collectors.toMap(
                        PluginNodeSchema::getId,          // Assuming `getId()` returns UUID
                        PluginNodeSchema::getConfigSchema
                ));
    }
}
