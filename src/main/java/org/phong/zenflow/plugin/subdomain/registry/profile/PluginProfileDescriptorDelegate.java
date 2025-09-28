package org.phong.zenflow.plugin.subdomain.registry.profile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.LoadSchemaHelper;
import org.phong.zenflow.plugin.subdomain.registry.Plugin;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Helper responsible for resolving profile descriptors for a plugin and building
 * the persisted plugin schema payload that captures descriptor metadata.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PluginProfileDescriptorDelegate {

    private final ApplicationContext applicationContext;

    public List<RegisteredPluginProfileDescriptor> resolveDescriptors(Class<?> pluginClass, Plugin annotation) {
        List<PluginProfileDescriptor> descriptors = extractDescriptors(pluginClass);
        if (descriptors.isEmpty()) {
            String legacySchemaPath = annotation.schemaPath().trim();
            if (!legacySchemaPath.isEmpty()) {
                descriptors = List.of(new LegacyPluginProfileDescriptor(annotation.name(), legacySchemaPath));
            }
        }
        if (descriptors.isEmpty()) {
            return List.of();
        }

        List<RegisteredPluginProfileDescriptor> registered = new ArrayList<>(descriptors.size());
        for (PluginProfileDescriptor descriptor : descriptors) {
            Map<String, Object> schema = loadDescriptorSchema(pluginClass, descriptor);
            Map<String, Object> defaults = descriptor.defaultValues();
            registered.add(new RegisteredPluginProfileDescriptor(descriptor, schema, defaults));
        }
        return registered;
    }

    private List<PluginProfileDescriptor> extractDescriptors(Class<?> pluginClass) {
        Object pluginBean = null;
        try {
            pluginBean = applicationContext.getBean(pluginClass);
        } catch (BeansException ex) {
            log.debug("Plugin class {} is not managed by Spring", pluginClass.getName());
        }

        if (pluginBean instanceof PluginProfileProvider provider) {
            List<PluginProfileDescriptor> descriptors = provider.getPluginProfiles();
            return descriptors == null ? List.of() : descriptors;
        }

        if (PluginProfileDescriptor.class.isAssignableFrom(pluginClass)) {
            try {
                PluginProfileDescriptor descriptor =
                        (PluginProfileDescriptor) pluginClass.getDeclaredConstructor().newInstance();
                return List.of(descriptor);
            } catch (ReflectiveOperationException e) {
                log.warn("Failed to instantiate plugin class {} as descriptor", pluginClass.getName(), e);
            }
        }

        return List.of();
    }

    private Map<String, Object> loadDescriptorSchema(Class<?> pluginClass, PluginProfileDescriptor descriptor) {
        String schemaPath = descriptor.schemaPath();
        if (schemaPath == null || schemaPath.trim().isEmpty()) {
            return Map.of();
        }
        try {
            return LoadSchemaHelper.loadSchema(pluginClass, schemaPath, "plugin.profile.schema.json");
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load profile schema for descriptor " + descriptor.id(), ex);
        }
    }

    private static final class LegacyPluginProfileDescriptor implements PluginProfileDescriptor {
        private final String displayName;
        private final String schemaPath;

        private LegacyPluginProfileDescriptor(String displayName, String schemaPath) {
            this.displayName = displayName == null || displayName.isBlank()
                    ? "Default Profile"
                    : displayName + " Profile";
            this.schemaPath = schemaPath;
        }

        @Override
        public String id() {
            return "default";
        }

        @Override
        public String displayName() {
            return displayName;
        }

        @Override
        public String schemaPath() {
            return schemaPath;
        }
    }
}
