package org.phong.zenflow.plugin.subdomain.registry.settings;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.LoadSchemaHelper;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PluginSettingDescriptorDelegate {

    private final ApplicationContext applicationContext;

    public List<RegisteredPluginSettingDescriptor> resolveDescriptors(Class<?> pluginClass) {
        List<PluginSettingDescriptor> descriptors = extractDescriptors(pluginClass);
        if (descriptors.isEmpty()) {
            return List.of();
        }

        List<RegisteredPluginSettingDescriptor> registered = new ArrayList<>(descriptors.size());
        for (PluginSettingDescriptor descriptor : descriptors) {
            Map<String, Object> schema = loadDescriptorSchema(pluginClass, descriptor);
            Map<String, Object> defaults = descriptor.defaultValues();
            registered.add(new RegisteredPluginSettingDescriptor(descriptor, schema, defaults));
        }
        return registered;
    }

    private List<PluginSettingDescriptor> extractDescriptors(Class<?> pluginClass) {
        Object pluginBean = null;
        try {
            pluginBean = applicationContext.getBean(pluginClass);
        } catch (BeansException ex) {
            log.debug("Plugin class {} is not managed by Spring", pluginClass.getName());
        }

        if (pluginBean instanceof PluginSettingProvider provider) {
            List<PluginSettingDescriptor> descriptors = provider.getPluginSettings();
            return descriptors == null ? List.of() : descriptors;
        }

        if (PluginSettingDescriptor.class.isAssignableFrom(pluginClass)) {
            try {
                PluginSettingDescriptor descriptor =
                        (PluginSettingDescriptor) pluginClass.getDeclaredConstructor().newInstance();
                return List.of(descriptor);
            } catch (ReflectiveOperationException e) {
                log.warn("Failed to instantiate plugin class {} as setting descriptor", pluginClass.getName(), e);
            }
        }

        return List.of();
    }

    private Map<String, Object> loadDescriptorSchema(Class<?> pluginClass, PluginSettingDescriptor descriptor) {
        String schemaPath = descriptor.schemaPath();
        if (schemaPath == null || schemaPath.trim().isEmpty()) {
            return Map.of();
        }
        try {
            return LoadSchemaHelper.loadSchema(pluginClass, schemaPath, "plugin.setting.schema.json");
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load setting schema for descriptor " + descriptor.id(), ex);
        }
    }
}
