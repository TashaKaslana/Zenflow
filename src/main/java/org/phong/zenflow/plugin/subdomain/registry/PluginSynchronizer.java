package org.phong.zenflow.plugin.subdomain.registry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.phong.zenflow.plugin.infrastructure.persistence.entity.Plugin;
import org.phong.zenflow.plugin.infrastructure.persistence.repository.PluginRepository;
import org.phong.zenflow.plugin.subdomain.registry.profile.PluginProfileDescriptorDelegate;
import org.phong.zenflow.plugin.subdomain.registry.profile.PluginProfileDescriptorRegistry;
import org.phong.zenflow.plugin.subdomain.registry.settings.PluginSettingDescriptorDelegate;
import org.phong.zenflow.plugin.subdomain.registry.settings.PluginSettingDescriptorRegistry;
import org.phong.zenflow.plugin.subdomain.registry.settings.RegisteredPluginSettingDescriptor;
import org.phong.zenflow.plugin.subdomain.registry.profile.RegisteredPluginProfileDescriptor;
import org.phong.zenflow.plugin.subdomain.schema.registry.SchemaIndexRegistry;
import org.phong.zenflow.plugin.subdomain.schema.services.SchemaValidator;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Scans the classpath for {@link org.phong.zenflow.plugin.subdomain.registry.Plugin}
 * annotations and synchronizes their metadata with the {@code plugins} table.
 * <p>
 * This synchronizer runs before the PluginNodeSynchronizer to ensure plugins are registered first.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(10) // Run before PluginNodeSynchronizer (order 20)
public class PluginSynchronizer implements ApplicationRunner {
    private static final String BUILTIN_SCHEMA_PREFIX = "builtin:";
    private static final String BASE_SCHEMA_NAME = "base_plugin_schema_definition";

    private final PluginRepository pluginRepository;
    private final SchemaValidator schemaValidator;
    private final SchemaIndexRegistry schemaIndexRegistry;
    private final PluginProfileDescriptorDelegate descriptorDelegate;
    private final PluginProfileDescriptorRegistry profileDescriptorRegistry;
    private final PluginSettingDescriptorDelegate settingDescriptorDelegate;
    private final PluginSettingDescriptorRegistry settingDescriptorRegistry;
    private final PluginSchemaComposer pluginSchemaComposer;

    @Override
    public void run(ApplicationArguments args) {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(
                org.phong.zenflow.plugin.subdomain.registry.Plugin.class));

        scanner.findCandidateComponents("org.phong.zenflow")
                .forEach(beanDefinition -> handleClass(beanDefinition.getBeanClassName()));
    }

    private void handleClass(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            org.phong.zenflow.plugin.subdomain.registry.Plugin annotation =
                    clazz.getAnnotation(org.phong.zenflow.plugin.subdomain.registry.Plugin.class);
            if (annotation == null) {
                return;
            }

            Plugin entity = pluginRepository.findByKey(annotation.key())
                    .orElseGet(Plugin::new);

            entity.setKey(annotation.key());
            entity.setName(annotation.name());
            entity.setVersion(annotation.version());
            entity.setDescription(annotation.description());
            entity.setTags(Arrays.asList(annotation.tags()));
            entity.setIcon(annotation.icon());
            entity.setOrganization(annotation.organization().isEmpty() ? null : annotation.organization());
            entity.setRegistryUrl(annotation.registryUrl().isEmpty() ? null : annotation.registryUrl());
            entity.setVerified(annotation.verified());
            entity.setPublisherId(UUID.fromString(annotation.publisherId()));

            List<RegisteredPluginProfileDescriptor> profileDescriptors =
                    descriptorDelegate.resolveDescriptors(clazz, annotation);
            List<RegisteredPluginSettingDescriptor> settingDescriptors =
                    settingDescriptorDelegate.resolveDescriptors(clazz);
            Map<String, Object> pluginSchema =
                    pluginSchemaComposer.compose(clazz, annotation, profileDescriptors, settingDescriptors);

            if (!pluginSchema.isEmpty()) {
                if (!schemaValidator.validate(
                        BUILTIN_SCHEMA_PREFIX + BASE_SCHEMA_NAME, new JSONObject(pluginSchema))) {
                    throw new IllegalStateException("Invalid plugin schema for " + className);
                }
                entity.setPluginSchema(pluginSchema);
            } else {
                entity.setPluginSchema(null);
            }

            Plugin savedEntity = pluginRepository.save(entity);
            log.info("Synchronized plugin: {} v{}", annotation.key(), annotation.version());

            profileDescriptorRegistry.register(savedEntity.getId(), savedEntity.getKey(), profileDescriptors);
            settingDescriptorRegistry.register(savedEntity.getId(), savedEntity.getKey(), settingDescriptors);
            indexSchema(savedEntity, clazz, annotation.schemaPath(), profileDescriptors, settingDescriptors);

        } catch (Exception e) {
            log.error("Failed to synchronize plugin for class {}", className, e);
        }
    }

    private void indexSchema(Plugin plugin, Class<?> clazz, String schemaPath, List<RegisteredPluginProfileDescriptor> profileDescriptors, List<RegisteredPluginSettingDescriptor> settingDescriptors) {
        UUID pluginId = plugin.getId();
        String indexKey = pluginId != null ? pluginId.toString() : plugin.getKey();

        if (indexKey == null || indexKey.isBlank()) {
            log.warn("Cannot index schema for plugin {} because it lacks an identifier.", plugin.getKey());
            return;
        }

        if (schemaPath != null && !schemaPath.trim().isEmpty()) {
            SchemaIndexRegistry.SchemaLocation location =
                    new SchemaIndexRegistry.SchemaLocation(clazz, schemaPath.trim());
            if (schemaIndexRegistry.addSchemaLocation(indexKey, location)) {
                log.debug("Indexed base plugin schema for {}", indexKey);
            }
        }

        if (profileDescriptors != null && !profileDescriptors.isEmpty()) {
            profileDescriptors.stream()
                .filter(registered -> registered != null && registered.descriptor() != null)
                .forEach(registered -> {
                    String descriptorSchemaPath = registered.descriptor().schemaPath();
                    if (descriptorSchemaPath == null || descriptorSchemaPath.isBlank()) {
                        return;
                    }
                    SchemaIndexRegistry.SchemaLocation location =
                            new SchemaIndexRegistry.SchemaLocation(clazz, descriptorSchemaPath.trim());
                    boolean added;
                    if (pluginId != null) {
                        added = schemaIndexRegistry.addProfileSchemaLocation(pluginId, registered.descriptor().id(), location);
                    } else {
                        added = schemaIndexRegistry.addProfileSchemaLocation(indexKey, registered.descriptor().id(), location);
                    }
                    if (added) {
                        log.debug("Indexed profile descriptor schema for plugin {} descriptor {}", indexKey, registered.descriptor().id());
                    }
                });
        }

        if (settingDescriptors != null && !settingDescriptors.isEmpty()) {
            settingDescriptors.stream()
                    .filter(registered -> registered != null && registered.descriptor() != null)
                    .forEach(registered -> {
                        String descriptorSchemaPath = registered.descriptor().schemaPath();
                        if (descriptorSchemaPath == null || descriptorSchemaPath.isBlank()) {
                            return;
                        }
                        SchemaIndexRegistry.SchemaLocation location =
                                new SchemaIndexRegistry.SchemaLocation(clazz, descriptorSchemaPath.trim());
                        boolean added;
                        if (pluginId != null) {
                            added = schemaIndexRegistry.addSettingSchemaLocation(pluginId, registered.descriptor().id(), location);
                        } else {
                            added = schemaIndexRegistry.addSettingSchemaLocation(indexKey, registered.descriptor().id(), location);
                        }
                        if (added) {
                            log.debug("Indexed setting descriptor schema for plugin {} descriptor {}", indexKey, registered.descriptor().id());
                        }
                    });
        }
    }
}
