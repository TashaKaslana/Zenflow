package org.phong.zenflow.plugin.subdomain.node.registry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.phong.zenflow.core.utils.LoadSchemaHelper;
import org.phong.zenflow.plugin.infrastructure.persistence.entity.Plugin;
import org.phong.zenflow.plugin.infrastructure.persistence.repository.PluginRepository;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.plugin.subdomain.execution.registry.PluginNodeExecutorRegistry;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.definition.adapter.NodeDefinitionExecutorAdapter;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.repository.PluginNodeRepository;
import org.phong.zenflow.plugin.subdomain.schema.registry.SchemaIndexRegistry;
import org.phong.zenflow.plugin.subdomain.schema.services.SchemaValidator;
import org.phong.zenflow.workflow.subdomain.trigger.registry.TriggerRegistry;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(20)
public class PluginNodeSynchronizer implements ApplicationRunner {
    private final static String SCHEMA_TEMPLATE = "builtin:plugin_node_definition_schema";

    private final PluginNodeRepository pluginNodeRepository;
    private final PluginRepository pluginRepository;
    private final TriggerRegistry triggerRegistry;
    private final PluginNodeExecutorRegistry registry;
    private final SchemaValidator schemaValidator;
    private final ApplicationContext applicationContext;
    private final SchemaIndexRegistry schemaIndexRegistry;

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

            Map<String, Object> schema = LoadSchemaHelper.loadSchema(
                    clazz, annotation.schemaPath().trim(), "schema.json"
            );
            if (!schema.isEmpty() && !schemaValidator.validate(
                    SCHEMA_TEMPLATE, new JSONObject(schema)
            )) {
                throw new IllegalStateException("Invalid plugin node schema for " + className);
            }

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

            SchemaIndexRegistry.SchemaLocation location =
                new SchemaIndexRegistry.SchemaLocation(clazz, annotation.schemaPath().trim());
            return schemaIndexRegistry.addSchemaLocation(nodeId, location);

        } catch (Exception e) {
            log.warn("Failed to index schema for class {}: {}", className, e.getMessage());
            return false;
        }
    }

    private void registerNodes(Class<?> clazz, PluginNode saved) {
        registry.register(
                saved.getId().toString(),
                () -> adaptExecutor(applicationContext.getBean(clazz))
        );

        if ("trigger".equalsIgnoreCase(saved.getType())) {
            triggerRegistry.registerTrigger(saved.getId().toString());
        }
    }

    private PluginNodeExecutor adaptExecutor(Object bean) {
        if (bean instanceof PluginNodeExecutor pluginExecutor) {
            return pluginExecutor;
        }
        if (bean instanceof NodeDefinitionProvider provider) {
            return new NodeDefinitionExecutorAdapter(provider);
        }
        throw new IllegalStateException(
                "@PluginNode annotated class must implement PluginNodeExecutor or NodeDefinitionProvider"
        );
    }
}

