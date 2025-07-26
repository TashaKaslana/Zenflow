package org.phong.zenflow.plugin.subdomain.node.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.log.auditlog.annotations.AuditLog;
import org.phong.zenflow.log.auditlog.enums.AuditAction;
import org.phong.zenflow.plugin.infrastructure.persistence.entity.Plugin;
import org.phong.zenflow.plugin.services.PluginService;
import org.phong.zenflow.plugin.subdomain.node.dto.CreatePluginNode;
import org.phong.zenflow.plugin.subdomain.node.dto.PluginNodeDto;
import org.phong.zenflow.plugin.subdomain.node.dto.UpdatePluginNodeRequest;
import org.phong.zenflow.plugin.subdomain.node.exception.PluginNodeException;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.mapstruct.PluginNodeMapper;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.repository.PluginNodeRepository;
import org.phong.zenflow.plugin.subdomain.node.utils.JsonSchemaValidator;
import org.phong.zenflow.plugin.subdomain.node.utils.SchemaRegistry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@AllArgsConstructor
@Slf4j
@Service
@Transactional(readOnly = true)
public class PluginNodeService {
    private final static String PLUGIN_NODE_DEFINITION_SCHEMA_NAME = "plugin_node_definition_schema";
    private final static String REMOTE_PLUGIN_NODE_DEFINITION_SCHEMA = "remote_plugin_node_definition_schema";

    private final PluginService pluginService;
    private final PluginNodeRepository pluginNodeRepository;
    private final PluginNodeMapper pluginNodeMapper;
    private final SchemaRegistry schemaRegistry;

    public PluginNode findById(UUID id) {
        return pluginNodeRepository.findById(id)
                .orElseThrow(() -> new PluginNodeException("PluginNode not found with id: " + id));
    }

    public PluginNodeDto findPluginNodeById(UUID id) {
        return pluginNodeMapper.toDto(findById(id));
    }

    @AuditLog(
            action = AuditAction.PLUGIN_NODE_CREATE,
            targetIdExpression = "returnObject.id"
    )
    @Transactional
    public PluginNodeDto createPluginNode(UUID pluginId, CreatePluginNode createPluginNode) {
        PluginNode pluginNode = pluginNodeMapper.toEntity(createPluginNode);
        JsonSchemaValidator.validate(
                schemaRegistry.getBuiltinSchema(
                        createPluginNode.executorType().equalsIgnoreCase("builtin") ?
                                PLUGIN_NODE_DEFINITION_SCHEMA_NAME : REMOTE_PLUGIN_NODE_DEFINITION_SCHEMA
                ),
                pluginNode.getConfigSchema()
        );
        Plugin plugin = pluginService.findPluginById(pluginId);
        pluginNode.setPlugin(plugin);
        return pluginNodeMapper.toDto(pluginNodeRepository.save(pluginNode));
    }

    public Page<PluginNodeDto> findAllByPluginId(Pageable pageable, UUID pluginId) {
        return pluginNodeRepository.findAllByPluginId(pluginId, pageable).map(pluginNodeMapper::toDto);
    }

    public Map<UUID, PluginNode> findAllByPluginId(List<UUID> nodeIds) {
        return pluginNodeRepository.findAllById(nodeIds)
                .stream()
                .collect(Collectors.toMap(PluginNode::getId, node -> node));
    }

    @AuditLog(
            action = AuditAction.PLUGIN_NODE_UPDATE,
            targetIdExpression = "returnObject.id"
    )
    @Transactional
    public PluginNodeDto updatePluginNode(UUID id, UpdatePluginNodeRequest updatePluginNodeRequest) {
        PluginNode existingPluginNode = findById(id);
        PluginNode updatedPluginNode = pluginNodeMapper.partialUpdate(updatePluginNodeRequest, existingPluginNode);
        JsonSchemaValidator.validate(
                schemaRegistry.getBuiltinSchema(
                        updatedPluginNode.getExecutorType().equalsIgnoreCase("builtin") ?
                                PLUGIN_NODE_DEFINITION_SCHEMA_NAME : REMOTE_PLUGIN_NODE_DEFINITION_SCHEMA
                ),
                updatedPluginNode.getConfigSchema()
        );
        return pluginNodeMapper.toDto(pluginNodeRepository.save(updatedPluginNode));
    }

    @AuditLog(
            action = AuditAction.PLUGIN_NODE_DELETE,
            targetIdExpression = "id"
    )
    @Transactional
    public void deletePluginNode(UUID id) {
        PluginNode pluginNode = findById(id);
        pluginNodeRepository.delete(pluginNode);
    }

    @AuditLog(
            action = AuditAction.PLUGIN_NODE_DELETE,
            targetIdExpression = "ids"
    )
    @Transactional
    public void deletePluginNodes(List<UUID> ids) {
        pluginNodeRepository.deleteAllById(ids);
    }

    public PluginNodeDto findPluginNodeByName(String name) {
        return pluginNodeMapper.toDto(pluginNodeRepository.findByName(name)
                .orElseThrow(() -> new PluginNodeException("PluginNode not found with name: " + name)));
    }
}
