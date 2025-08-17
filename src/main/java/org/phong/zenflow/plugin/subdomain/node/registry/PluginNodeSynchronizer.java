package org.phong.zenflow.plugin.subdomain.node.registry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.infrastructure.persistence.entity.Plugin;
import org.phong.zenflow.plugin.infrastructure.persistence.repository.PluginRepository;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.repository.PluginNodeRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

/**
 * Scans the classpath for {@link org.phong.zenflow.plugin.subdomain.node.registry.PluginNode}
 * annotations and synchronizes their metadata with the {@code plugin_nodes} table.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PluginNodeSynchronizer implements ApplicationRunner {

    private final PluginNodeRepository pluginNodeRepository;
    private final PluginRepository pluginRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(
                org.phong.zenflow.plugin.subdomain.node.registry.PluginNode.class));

        scanner.findCandidateComponents("org.phong.zenflow")
                .forEach(beanDefinition -> handleClass(beanDefinition.getBeanClassName()));
    }

    private void handleClass(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            org.phong.zenflow.plugin.subdomain.node.registry.PluginNode annotation =
                    clazz.getAnnotation(
                            org.phong.zenflow.plugin.subdomain.node.registry.PluginNode.class);
            if (annotation == null) {
                return;
            }

            String[] parts = annotation.key().split(":", 2);
            if (parts.length != 2) {
                log.warn("Invalid plugin node key {} on class {}", annotation.key(), className);
                return;
            }
            String pluginKey = parts[0];
            String nodeKey = parts[1];

            Map<String, Object> schema = loadSchema(clazz, annotation.schemaPath().trim());

            Plugin plugin = pluginRepository.findByKey(pluginKey)
                    .orElseThrow(() -> new IllegalStateException("Plugin not found: " + pluginKey));

            PluginNode entity = pluginNodeRepository.findByKey(nodeKey)
                    .orElseGet(PluginNode::new);

            entity.setPlugin(plugin);
            entity.setKey(nodeKey);
            entity.setName(annotation.name());
            entity.setType(annotation.type());
            entity.setPluginNodeVersion(annotation.version());
            entity.setDescription(annotation.description());
            entity.setIcon(annotation.icon());
            entity.setTags(Arrays.asList(annotation.tags()));
            entity.setExecutorType(annotation.executor());
            entity.setConfigSchema(schema);

            pluginNodeRepository.save(entity);
        } catch (Exception e) {
            log.error("Failed to synchronize plugin node for class {}", className, e);
        }
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
}
