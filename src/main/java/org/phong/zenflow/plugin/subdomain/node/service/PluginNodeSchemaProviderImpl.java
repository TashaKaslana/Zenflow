package org.phong.zenflow.plugin.subdomain.node.service;

import org.phong.zenflow.plugin.subdomain.node.exception.ValidateNodeSchemaException;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.projections.PluginNodeSchema;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.repository.PluginNodeRepository;
import org.phong.zenflow.plugin.subdomain.node.interfaces.PluginNodeSchemaProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PluginNodeSchemaProviderImpl implements PluginNodeSchemaProvider {

    private final PluginNodeRepository pluginNodeRepository;

    public PluginNodeSchemaProviderImpl(PluginNodeRepository pluginNodeRepository) {
        this.pluginNodeRepository = pluginNodeRepository;
    }

    @Override
    public Map<String, Object> getSchemaJson(String key) {
        return pluginNodeRepository
                .findByKey(key)
                .orElseThrow(
                        () -> new ValidateNodeSchemaException("Node schema not found for nodeKey: " +  key)
                ).getConfigSchema();
    }

    @Override
    public Map<String, Map<String, Object>> getAllSchemasByIdentifiers(List<String> keyList) {
        return pluginNodeRepository.findAllByKeyList(keyList)
                .stream()
                .filter(node -> node.getConfigSchema() != null)
                .collect(Collectors.toMap(
                        PluginNodeSchema::getKey,
                        PluginNodeSchema::getConfigSchema
                ));
    }
}
