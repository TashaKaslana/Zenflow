package org.phong.zenflow.plugin.subdomain.registry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.phong.zenflow.plugin.infrastructure.persistence.entity.Plugin;
import org.phong.zenflow.plugin.infrastructure.persistence.repository.PluginRepository;
import org.phong.zenflow.plugin.subdomain.registry.profile.PluginProfileDescriptorDelegate;
import org.phong.zenflow.plugin.subdomain.registry.profile.PluginProfileDescriptorRegistry;
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

            List<RegisteredPluginProfileDescriptor> descriptors =
                    descriptorDelegate.resolveDescriptors(clazz, annotation);
            Map<String, Object> pluginSchema =
                    descriptorDelegate.buildPluginSchema(clazz, annotation, descriptors);

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

            profileDescriptorRegistry.register(savedEntity.getId(), savedEntity.getKey(), descriptors);
            indexSchema(savedEntity, clazz, annotation.schemaPath());

        } catch (Exception e) {
            log.error("Failed to synchronize plugin for class {}", className, e);
        }
    }

    private void indexSchema(Plugin plugin, Class<?> clazz, String schemaPath) {
        if (plugin.getId() == null) {
            log.warn("Cannot index schema for plugin {} because it has no ID.", plugin.getKey());
            return;
        }
        if (schemaPath == null || schemaPath.trim().isEmpty()) {
            return;
        }
        String pluginId = plugin.getId().toString();
        SchemaIndexRegistry.SchemaLocation location =
                new SchemaIndexRegistry.SchemaLocation(clazz, schemaPath.trim());
        if (schemaIndexRegistry.addSchemaLocation(pluginId, location)) {
            log.debug("Indexed schema location for plugin ID {}", pluginId);
        }
    }
}
