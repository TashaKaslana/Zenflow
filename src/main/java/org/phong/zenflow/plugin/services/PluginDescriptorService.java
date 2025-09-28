package org.phong.zenflow.plugin.services;

import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.phong.zenflow.plugin.exception.PluginException;
import org.phong.zenflow.plugin.infrastructure.persistence.entity.Plugin;
import org.phong.zenflow.plugin.infrastructure.persistence.repository.PluginRepository;
import org.phong.zenflow.plugin.subdomain.registry.PluginDescriptorSection;
import org.phong.zenflow.plugin.subdomain.schema.services.SchemaRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PluginDescriptorService {

    private final PluginRepository pluginRepository;
    private final SchemaRegistry schemaRegistry;

    public Map<String, Object> getDescriptorSchema(String key, String descriptorId, PluginDescriptorSection section) {
        Plugin plugin = pluginRepository.findByKey(key)
                .orElseThrow(() -> new PluginException("Plugin not found with key: " + key));

        JSONObject schemaJson = switch (section) {
            case PROFILE -> schemaRegistry.getPluginProfileDescriptorSchema(plugin.getId(), key, descriptorId);
            case SETTING -> schemaRegistry.getPluginSettingDescriptorSchema(plugin.getId(), key, descriptorId);
        };

        if (schemaJson == null) {
            throw new PluginException("Descriptor schema not found for id: " + descriptorId);
        }
        return schemaJson.toMap();
    }
}
