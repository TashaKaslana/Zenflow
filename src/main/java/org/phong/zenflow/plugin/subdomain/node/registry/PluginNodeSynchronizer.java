package org.phong.zenflow.plugin.subdomain.node.registry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.infrastructure.persistence.entity.Plugin;
import org.phong.zenflow.plugin.infrastructure.persistence.repository.PluginRepository;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.plugin.subdomain.execution.registry.PluginNodeExecutorRegistry;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.repository.PluginNodeRepository;
import org.phong.zenflow.workflow.subdomain.trigger.registry.TriggerRegistry;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scans the classpath for {@link org.phong.zenflow.plugin.subdomain.node.registry.PluginNode}
 * annotations and synchronizes their metadata with the {@code plugin_nodes} table.
 * Also builds a schema index for fast schema location lookup by UUID.
 * <p>
 * This synchronizer runs after the PluginSynchronizer to ensure plugins are registered first.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(20) // Run after PluginSynchronizer (order 10)
public class PluginNodeSynchronizer implements ApplicationRunner {

    private final PluginNodeRepository pluginNodeRepository;
    private final PluginRepository pluginRepository;
    private final ObjectMapper objectMapper;
    private final TriggerRegistry triggerRegistry;
    private final PluginNodeExecutorRegistry registry;
    private final ApplicationContext applicationContext;


    // Schema index for fast lookups by UUID
    @Getter
    private final ConcurrentHashMap<String, SchemaLocation> schemaIndex = new ConcurrentHashMap<>();

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting plugin node synchronization and schema indexing...");
        long startTime = System.currentTimeMillis();

        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(
                org.phong.zenflow.plugin.subdomain.node.registry.PluginNode.class));

        int synchronizedIndex = 0;
        int indexed = 0;

        for (var beanDefinition : scanner.findCandidateComponents("org.phong.zenflow")) {
            String className = beanDefinition.getBeanClassName();
            PluginNode savedNode = handleClass(className);
            if (savedNode != null) {
                synchronizedIndex++;
                if (indexSchema(className, savedNode.getId().toString())) {
                    indexed++;
                }
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Plugin node synchronization completed: {} nodes synchronized, {} schemas indexed in {}ms",
                synchronizedIndex, indexed, duration);
    }

    private PluginNode handleClass(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            org.phong.zenflow.plugin.subdomain.node.registry.PluginNode annotation =
                    clazz.getAnnotation(
                            org.phong.zenflow.plugin.subdomain.node.registry.PluginNode.class);
            if (annotation == null) {
                return null;
            }

            String[] parts = annotation.key().split(":", 2);
            if (parts.length != 2) {
                log.warn("Invalid plugin node key {} on class {}", annotation.key(), className);
                return null;
            }
            String pluginKey = parts[0];
            String compositeKey = annotation.key() + ':' + annotation.version();

            Map<String, Object> schema = loadSchema(clazz, annotation.schemaPath().trim());

            Plugin plugin = pluginRepository.getReferenceByKey(pluginKey)
                    .orElseThrow(() -> new IllegalStateException("Plugin not found with composite key: " + compositeKey));

            PluginNode entity = pluginNodeRepository.findByCompositeKey(compositeKey)
                    .orElseGet(PluginNode::new);

            entity.setPlugin(plugin);
            entity.setCompositeKey(compositeKey);
            entity.setName(annotation.name());
            entity.setType(annotation.type());
            entity.setPluginNodeVersion(annotation.version());
            entity.setDescription(annotation.description());
            entity.setIcon(annotation.icon());
            entity.setTags(Arrays.asList(annotation.tags()));
            entity.setExecutorType(annotation.executor());
            entity.setConfigSchema(schema);

            PluginNode saved = pluginNodeRepository.save(entity);
            log.info("Synchronized plugin node with composite key: {}", compositeKey);

            registerNodes(clazz, saved);
            return saved;
        } catch (Exception e) {
            log.error("Failed to synchronize plugin node for class {}", className, e);
            return null;
        }
    }

    private boolean indexSchema(String className, String nodeId) {
        try {
            Class<?> clazz = Class.forName(className);
            org.phong.zenflow.plugin.subdomain.node.registry.PluginNode annotation =
                    clazz.getAnnotation(
                            org.phong.zenflow.plugin.subdomain.node.registry.PluginNode.class);
            if (annotation == null) {
                return false;
            }

            SchemaLocation location = new SchemaLocation(clazz, annotation.schemaPath().trim());
            schemaIndex.put(nodeId, location);

            log.debug("Indexed schema location for node ID {}: {}", nodeId, clazz.getName());
            return true;

        } catch (Exception e) {
            log.warn("Failed to index schema for class {}: {}", className, e.getMessage());
            return false;
        }
    }

    private void registerNodes(Class<?> clazz, PluginNode saved) {
        PluginNodeExecutor instance = applicationContext.getBean(clazz.asSubclass(PluginNodeExecutor.class));

        registry.register(
                saved.getId().toString(),
                () -> instance
        );

        if ("trigger".equalsIgnoreCase(saved.getType())) {
            triggerRegistry.registerTrigger(saved.getId().toString());
        }
    }

    // Schema index access methods
    public SchemaLocation getSchemaLocation(String nodeId) {
        return schemaIndex.get(nodeId);
    }

    public boolean hasSchemaLocation(String nodeId) {
        return schemaIndex.containsKey(nodeId);
    }

    public int getSchemaIndexSize() {
        return schemaIndex.size();
    }

    public Map<String, Object> loadSchema(Class<?> clazz, String customPath) {
        String resourcePath = extractPath(clazz, customPath);
        String classpathResource = "/" + resourcePath;

        // 1. Try classpath first (works for packaged nodes)
        InputStream classpathStream = clazz.getResourceAsStream(classpathResource);
        if (classpathStream != null) {
            try (InputStream is = classpathStream) {
                return objectMapper.readValue(is, new TypeReference<>() {});
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load schema from classpath for " + clazz.getName(), e);
            }
        }

        // 2. Fallback to file system (works for external plugin nodes)
        Path filePath = Paths.get(resourcePath);
        if (Files.exists(filePath)) {
            try (InputStream is = Files.newInputStream(filePath)) {
                return objectMapper.readValue(is, new TypeReference<>() {});
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load schema from filesystem for " + clazz.getName(), e);
            }
        }

        log.warn("No schema.json found for {}", clazz.getName());
        return Map.of();
    }

    public String extractPath(Class<?> clazz, String customPath) {
        Path basePath = Paths.get(clazz.getPackageName().replace('.', '/'));

        Path resultPath;
        if (customPath.isEmpty()) {
            resultPath = basePath.resolve("schema.json");
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

    public record SchemaLocation(Class<?> clazz, String schemaPath) {
        public SchemaLocation {
            if (clazz == null) {
                throw new IllegalArgumentException("Class cannot be null");
            }
            if (schemaPath == null) {
                schemaPath = "";
            }
        }

        public boolean hasCustomPath() {
            return !schemaPath.isEmpty();
        }
    }
}
