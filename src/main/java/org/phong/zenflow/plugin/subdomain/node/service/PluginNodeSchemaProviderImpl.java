package org.phong.zenflow.plugin.subdomain.node.service;

import org.phong.zenflow.plugin.subdomain.node.exception.ValidateNodeSchemaException;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.repository.PluginNodeRepository;
import org.phong.zenflow.plugin.subdomain.node.interfaces.PluginNodeSchemaProvider;
import org.springframework.stereotype.Service;

@Service
public class PluginNodeSchemaProviderImpl implements PluginNodeSchemaProvider {

    private final PluginNodeRepository pluginNodeRepository;

    public PluginNodeSchemaProviderImpl(PluginNodeRepository pluginNodeRepository) {
        this.pluginNodeRepository = pluginNodeRepository;
    }

    @Override
    public String getSchemaJson(String plugin, String node) {
        return pluginNodeRepository
                .findByPluginNameAndName(plugin, node)
                .map(PluginNode::getConfigSchema).orElseThrow(
                        () -> new ValidateNodeSchemaException("Node schema not found for plugin: " + plugin + ", node: " + node)
                ).toString();
    }
}
