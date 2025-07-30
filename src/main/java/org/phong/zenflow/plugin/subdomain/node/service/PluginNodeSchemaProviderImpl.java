package org.phong.zenflow.plugin.subdomain.node.service;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.exception.ValidateNodeSchemaException;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.repository.PluginNodeRepository;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.repository.PluginNodeSpecifications;
import org.phong.zenflow.plugin.subdomain.node.interfaces.PluginNodeSchemaProvider;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class PluginNodeSchemaProviderImpl implements PluginNodeSchemaProvider {

    private final PluginNodeRepository pluginNodeRepository;

    @Override
    public Map<String, Object> getSchemaJson(String key) {
        return pluginNodeRepository
                .findByKey(key)
                .orElseThrow(
                        () -> new ValidateNodeSchemaException("Node schema not found for nodeKey: " +  key)
                ).getConfigSchema();
    }

    @Override
    public Map<String, Map<String, Object>> getAllSchemasByIdentifiers(List<PluginNodeIdentifier> identifiers) {
        Specification<PluginNode> spec = PluginNodeSpecifications.withIdentifiers(identifiers);
        return pluginNodeRepository.findAll(spec).stream()
                .filter(node -> node.getConfigSchema() != null)
                .collect(Collectors.toMap(
                        this::getCacheKey,
                        PluginNode::getConfigSchema
                ));
    }

    private String getCacheKey(PluginNode entity) {
        return entity.getPlugin().getName() + ":" + entity.getKey() + ":" + entity.getPluginNodeVersion();
    }
}
