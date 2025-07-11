package org.phong.zenflow.plugin.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.log.auditlog.annotations.AuditLog;
import org.phong.zenflow.log.auditlog.enums.AuditAction;
import org.phong.zenflow.plugin.dto.CreatePluginRequest;
import org.phong.zenflow.plugin.dto.PluginDto;
import org.phong.zenflow.plugin.dto.UpdatePluginRequest;
import org.phong.zenflow.plugin.exception.PluginException;
import org.phong.zenflow.plugin.infrastructure.mapstruct.PluginMapper;
import org.phong.zenflow.plugin.infrastructure.persistence.entity.Plugin;
import org.phong.zenflow.plugin.infrastructure.persistence.repository.PluginRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Slf4j
@Service
@Transactional(readOnly = true)
public class PluginService {
    private final PluginMapper pluginMapper;
    private final PluginRepository pluginRepository;

    @AuditLog(
            action = AuditAction.PLUGIN_UPLOAD,
            targetIdExpression = "returnObject.id"
    )
    @Transactional
    public PluginDto savePlugin(CreatePluginRequest createPluginRequest) {
        Plugin plugin = pluginMapper.toEntity(createPluginRequest);
        Plugin savedPlugin = pluginRepository.save(plugin);
        return pluginMapper.toDto(savedPlugin);
    }

    public Page<PluginDto> getAllPlugins(Pageable pageable) {
        return pluginRepository.findAll(pageable)
                .map(pluginMapper::toDto);
    }

    public PluginDto getPluginByName(String name) {
        return pluginRepository.findByName(name)
                .map(pluginMapper::toDto)
                .orElseThrow(() -> new PluginException("Plugin not found with name: " + name));
    }

    public PluginDto getPluginById(UUID id) {
        return pluginRepository.findById(id)
                .map(pluginMapper::toDto)
                .orElseThrow(() -> new PluginException("Plugin not found with id: " + id));
    }

    @Transactional
    public PluginDto updatePlugin(UUID id, UpdatePluginRequest updatePluginRequest) {
        Plugin existingPlugin = pluginRepository.findById(id)
                .orElseThrow(() -> new PluginException("Plugin not found with id: " + id));

        Plugin updatedPlugin = pluginMapper.partialUpdate(updatePluginRequest, existingPlugin);

        Plugin savedPlugin = pluginRepository.save(updatedPlugin);
        return pluginMapper.toDto(savedPlugin);
    }

    @AuditLog(
            action = AuditAction.PLUGIN_DELETE,
            targetIdExpression = "id"
    )
    @Transactional
    public void deletePlugin(UUID id) {
        Plugin plugin = pluginRepository.findById(id)
                .orElseThrow(() -> new PluginException("Plugin not found with id: " + id));
        pluginRepository.delete(plugin);
    }

    @Transactional
    @AuditLog(
            action = AuditAction.PLUGIN_DELETE,
            description = "Delete bulk plugins"
    )
    public void deletePluginsByIds(List<UUID> ids) {
        pluginRepository.deleteAllById(ids);
    }

    @Transactional
    public void deleteAllPlugins() {
        pluginRepository.deleteAll();
    }

    public boolean pluginExists(String name) {
        return pluginRepository.findByName(name).isPresent();
    }

    public Plugin findPluginById(UUID id) {
        return pluginRepository.findById(id)
                .orElseThrow(() -> new PluginException("Plugin not found with id: " + id));
    }
}
