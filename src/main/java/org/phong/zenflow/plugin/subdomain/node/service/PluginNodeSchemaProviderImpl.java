package org.phong.zenflow.plugin.subdomain.node.service;

import org.phong.zenflow.plugin.subdomain.node.exception.ValidateNodeSchemaException;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.repository.PluginNodeRepository;
import org.phong.zenflow.plugin.subdomain.node.interfaces.PluginNodeSchemaProvider;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PluginNodeSchemaProviderImpl implements PluginNodeSchemaProvider {

    private final PluginNodeRepository pluginNodeRepository;

    public PluginNodeSchemaProviderImpl(PluginNodeRepository pluginNodeRepository) {
        this.pluginNodeRepository = pluginNodeRepository;
    }

    @Override
    public String getSchemaJson(UUID pluginId, UUID nodeId) {
        return pluginNodeRepository
                .findByPluginIdAndId(pluginId, nodeId)
                .map(PluginNode::getConfigSchema).orElseThrow(
                        () -> new ValidateNodeSchemaException("Node schema not found for pluginId: " + pluginId + ", nodeId: " + nodeId)
                ).toString();
    }
}
