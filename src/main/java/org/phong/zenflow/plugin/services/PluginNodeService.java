package org.phong.zenflow.plugin.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.log.auditlog.annotations.AuditLog;
import org.phong.zenflow.log.auditlog.enums.AuditAction;
import org.phong.zenflow.plugin.dto.CreatePluginNode;
import org.phong.zenflow.plugin.dto.UpdatePluginNodeRequest;
import org.phong.zenflow.plugin.exception.PluginNodeException;
import org.phong.zenflow.plugin.infrastructure.mapstruct.PluginNodeMapper;
import org.phong.zenflow.plugin.infrastructure.persistence.entity.Plugin;
import org.phong.zenflow.plugin.infrastructure.persistence.entity.PluginNode;
import org.phong.zenflow.plugin.infrastructure.persistence.repository.PluginNodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Slf4j
@Service
@Transactional(readOnly = true)
public class PluginNodeService {
    private final PluginService pluginService;
    private final PluginNodeRepository pluginNodeRepository;
    private final PluginNodeMapper pluginNodeMapper;

    public PluginNode findById(UUID id) {
        return pluginNodeRepository.findById(id)
                .orElseThrow(() -> new PluginNodeException("PluginNode not found with id: " + id));
    }

    @AuditLog(
            action = AuditAction.PLUGIN_NODE_CREATE,
            targetIdExpression = "returnObject.id"
    )
    @Transactional
    public PluginNode createPluginNode(UUID pluginId, CreatePluginNode createPluginNode) {
        PluginNode pluginNode = pluginNodeMapper.toEntity(createPluginNode);
        Plugin plugin = pluginService.findPluginById(pluginId);
        pluginNode.setPlugin(plugin);
        return pluginNodeRepository.save(pluginNode);
    }

    @AuditLog(
            action = AuditAction.PLUGIN_NODE_UPDATE,
            targetIdExpression = "returnObject.id"
    )
    @Transactional
    public PluginNode updatePluginNode(UUID id, UpdatePluginNodeRequest updatePluginNodeRequest) {
        PluginNode existingPluginNode = findById(id);
        PluginNode updatedPluginNode = pluginNodeMapper.partialUpdate(updatePluginNodeRequest, existingPluginNode);

        return pluginNodeRepository.save(updatedPluginNode);
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

    public PluginNode findPluginNodeByName(String name) {
        return pluginNodeRepository.findByName(name)
                .orElseThrow(() -> new PluginNodeException("PluginNode not found with name: " + name));
    }
}
