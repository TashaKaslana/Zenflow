package org.phong.zenflow.plugin.subdomain.node.registry;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class PluginNodeSchemaIndex implements ApplicationRunner {

    private final ConcurrentHashMap<String, SchemaLocation> schemaIndex = new ConcurrentHashMap<>();

    @PostConstruct
    public void buildIndex() {
        log.info("Building plugin node schema index...");
        long startTime = System.currentTimeMillis();

        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(PluginNode.class));

        int indexed = 0;
        for (var beanDefinition : scanner.findCandidateComponents("org.phong.zenflow")) {
            if (indexClass(beanDefinition.getBeanClassName())) {
                indexed++;
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Schema index built: {} entries in {}ms", indexed, duration);
    }

    @Override
    public void run(ApplicationArguments args) {
        // Delegate to the same method — ensures consistency
        buildIndex();
    }

    private boolean indexClass(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            PluginNode annotation = clazz.getAnnotation(PluginNode.class);
            if (annotation == null) {
                return false;
            }

            String[] parts = annotation.key().split(":", 2);
            if (parts.length != 2) {
                log.warn("Invalid plugin node key {} on class {}", annotation.key(), className);
                return false;
            }

            String pluginKey = parts[0];
            String nodeKey = parts[1];
            PluginNodeIdentifier identifier = new PluginNodeIdentifier(
                    pluginKey, nodeKey, annotation.version(), annotation.executor()
            );

            SchemaLocation location = new SchemaLocation(clazz, annotation.schemaPath().trim());
            schemaIndex.put(identifier.toCacheKey(), location);

            log.debug("Indexed schema location for {}: {}", identifier.toCacheKey(), clazz.getName());
            return true;

        } catch (Exception e) {
            log.warn("Failed to index schema for class {}: {}", className, e.getMessage());
            return false;
        }
    }

    public SchemaLocation getSchemaLocation(PluginNodeIdentifier identifier) {
        return schemaIndex.get(identifier.toCacheKey());
    }

    public boolean hasSchemaLocation(PluginNodeIdentifier identifier) {
        return schemaIndex.containsKey(identifier.toCacheKey());
    }

    public int getIndexSize() {
        return schemaIndex.size();
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
