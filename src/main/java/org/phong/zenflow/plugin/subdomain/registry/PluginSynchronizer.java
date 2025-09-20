package org.phong.zenflow.plugin.subdomain.registry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.phong.zenflow.plugin.infrastructure.persistence.entity.Plugin;
import org.phong.zenflow.plugin.infrastructure.persistence.repository.PluginRepository;
import org.phong.zenflow.plugin.subdomain.schema.registry.SchemaIndexRegistry;
import org.phong.zenflow.plugin.subdomain.schema.services.SchemaValidator;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
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
    private final static String BUILTIN_SCHEMA_PREFIX = "builtin:";
    private final static String BASE_SCHEMA_NAME = "base_plugin_schema_definition";

    private final PluginRepository pluginRepository;
    private final ObjectMapper objectMapper;
    private final SchemaValidator schemaValidator;
    private final SchemaIndexRegistry schemaIndexRegistry;

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
                    clazz.getAnnotation(
                            org.phong.zenflow.plugin.subdomain.registry.Plugin.class);
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
            String schemaPath = annotation.schemaPath().trim();

            Map<String, Object> pluginSchema = schemaPath.isEmpty()
                    ? null
                    : loadSchema(clazz, schemaPath);
            if (pluginSchema != null && !schemaValidator.validate(
                    BUILTIN_SCHEMA_PREFIX + BASE_SCHEMA_NAME, new JSONObject(pluginSchema)
            )) {
                throw new IllegalStateException("Invalid plugin schema for " + className);
            }

            entity.setPluginSchema(pluginSchema);

            Plugin savedEntity = pluginRepository.save(entity);
            log.info("Synchronized plugin: {} v{}", annotation.key(), annotation.version());

            indexSchema(savedEntity, clazz, schemaPath);

        } catch (Exception e) {
            log.error("Failed to synchronize plugin for class {}", className, e);
        }
    }

    private void indexSchema(Plugin plugin, Class<?> clazz, String schemaPath) {
        if (schemaPath == null || schemaPath.trim().isEmpty()) {
            return; // Nothing to index
        }
        if (plugin.getId() == null) {
            log.warn("Cannot index schema for plugin {} because it has no ID.", plugin.getKey());
            return;
        }

        try {
            String pluginId = plugin.getId().toString();
            SchemaIndexRegistry.SchemaLocation location =
                    new SchemaIndexRegistry.SchemaLocation(clazz, schemaPath.trim());

            if (schemaIndexRegistry.addSchemaLocation(pluginId, location)) {
                log.debug("Indexed schema location for plugin ID {}", pluginId);
            }
        } catch (Exception e) {
            log.error("Failed to index schema for plugin class {}: {}", clazz.getName(), e.getMessage());
        }
    }

    private Map<String, Object> loadSchema(Class<?> clazz, String customPath) {
        String resourcePath = extractPath(clazz, customPath);
        String classpathResource = "/" + resourcePath;

        InputStream classpathStream = clazz.getResourceAsStream(classpathResource);
        if (classpathStream != null) {
            try (InputStream is = classpathStream) {
                Map<String, Object> schema = objectMapper.readValue(is, new TypeReference<>() {});
                return schema.isEmpty() ? null : schema;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load plugin schema from classpath for " + clazz.getName(), e);
            }
        }

        Path filePath = Paths.get(resourcePath);
        if (Files.exists(filePath)) {
            try (InputStream is = Files.newInputStream(filePath)) {
                Map<String, Object> schema = objectMapper.readValue(is, new TypeReference<>() {});
                return schema.isEmpty() ? null : schema;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load plugin schema from filesystem for " + clazz.getName(), e);
            }
        }

        log.warn("No plugin schema found for {} at {}", clazz.getName(), resourcePath);
        return null;
    }

    private String extractPath(Class<?> clazz, String customPath) {
        Path basePath = Paths.get(clazz.getPackageName().replace('.', '/'));

        Path resultPath;
        if (customPath.isEmpty()) {
            resultPath = basePath.resolve("plugin.schema.json");
        } else if (customPath.startsWith("/")) {
            resultPath = basePath.resolve(customPath.substring(1));
        } else if (customPath.startsWith("./")) {
            resultPath = basePath.resolve(customPath.substring(2));
        } else if (customPath.startsWith("../")) {
            resultPath = basePath.resolve(customPath).normalize();
        } else {
            resultPath = basePath.resolve(customPath);
        }

        return resultPath.toString().replace("\\", "/");
    }
}