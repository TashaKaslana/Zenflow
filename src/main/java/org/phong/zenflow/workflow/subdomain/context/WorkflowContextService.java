package org.phong.zenflow.workflow.subdomain.context;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.schema.services.SchemaRegistry;
import org.phong.zenflow.workflow.subdomain.execution.services.TemplateService;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowNodes;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.OutputUsage;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowMetadata;

import org.phong.zenflow.workflow.subdomain.schema_validator.service.SchemaTypeResolver;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Static context service for workflow nodes.
 * <p>
 * This service builds a static context for workflow nodes, resolving dependencies and consumer relationships
 * based on the node configurations and existing metadata.
 * </p>
 * <p>It also handles aliases resolution and type inference.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WorkflowContextService {
    private final SchemaRegistry schemaRegistry;
    private final SchemaTypeResolver schemaTypeResolver;
    private final TemplateService templateService;

    private void generateAliasLinkBackToConsumers(WorkflowMetadata ctx) {
        for (Map.Entry<String, String> aliasEntry : ctx.aliases().entrySet()) {
            String aliasName = aliasEntry.getKey();
            templateService.extractRefs(aliasEntry.getValue()).stream().findFirst().ifPresent(originalRef -> ctx.nodeConsumers()
                    .computeIfAbsent(originalRef, k -> new OutputUsage())
                    .getAliases()
                    .add(aliasName));
        }
    }

    public WorkflowMetadata buildStaticContext(WorkflowDefinition wf) {
        WorkflowMetadata ctx = new WorkflowMetadata();
        WorkflowMetadata existingMetadata = wf.metadata();

        // Preserve existing aliases from metadata
        if (existingMetadata != null && existingMetadata.aliases() != null) {
            ctx.aliases().putAll(ObjectConversion.safeConvert(existingMetadata.aliases(), new TypeReference<>() {
            }));
        }

        generateDependenciesAndConsumers(wf.nodes(), ctx);
        generateAliasLinkBackToConsumers(ctx);
        generateTypeForConsumerFields(wf, ctx);

        return new WorkflowMetadata(ctx.aliases(), ctx.nodeDependencies(), ctx.nodeConsumers());
    }

    private void generateDependenciesAndConsumers(WorkflowNodes nodes, WorkflowMetadata ctx) {
        nodes.forEach((nodeKey, node) -> {
            Map<String, Object> input = node.getConfig().input();
            if (input == null || input.isEmpty()) {
                return;
            }

            for (Map.Entry<String, Object> entry : input.entrySet()) {
                Object inputValue = entry.getValue();
                if (inputValue == null) {
                    continue;
                }

                Set<String> referenced = templateService.extractRefs(inputValue.toString());

                for (String ref : referenced) {
                    String resolvedRef = resolveAlias(ref, ctx.aliases());

                    ctx.nodeDependencies()
                            .computeIfAbsent(nodeKey, k -> new HashSet<>())
                            .add(resolvedRef);

                    ctx.nodeConsumers()
                            .computeIfAbsent(resolvedRef, k -> new OutputUsage())
                            .getConsumers()
                            .add(nodeKey);
                }
            }
        });
    }

    private void generateTypeForConsumerFields(WorkflowDefinition wf, WorkflowMetadata ctx) {
        Map<String, BaseWorkflowNode> nodeMap = wf.nodes().asMap();
        Set<String> pluginNodeIds = wf.nodes().getPluginNodeIds().stream()
                .map(UUID::toString)
                .collect(Collectors.toSet());
        Map<String, JSONObject> schemas = schemaRegistry.getSchemaMapByTemplateStrings(pluginNodeIds);

        ctx.nodeConsumers().forEach((key, usage) -> {
            String[] parts = key.split("\\.output\\.");
            if (parts.length != 2) {
                return; // Not an output field, so we can't determine its type from a schema.
            }

            String nodeKey = parts[0];
            String outputFieldName = parts[1];

            BaseWorkflowNode node = nodeMap.get(nodeKey);
            if (node == null) {
                log.warn("Node with key {} not found in existing nodes, skipping type generation for consumer field {}", nodeKey, key);
                return;
            }

            String schemaKey = node.getPluginNode().getNodeId().toString();
            JSONObject schema = schemas.get(schemaKey);

            populateTypeNodeConsumer(key, usage, schema, outputFieldName, nodeKey);
        });
    }

    private void populateTypeNodeConsumer(String key, OutputUsage usage, JSONObject schema, String outputFieldName, String nodeKey) {
        if (schema != null) {
            try {
                if (schema.has("properties")) {
                    JSONObject rootProperties = schema.getJSONObject("properties");
                    if (rootProperties.has("output") && rootProperties.getJSONObject("output").has("properties")) {
                        JSONObject outputProperties = rootProperties.getJSONObject("output").getJSONObject("properties");
                        String[] fieldParts = outputFieldName.split("\\.");
                        JSONObject currentSchema = outputProperties;

                        for (int i = 0; i < fieldParts.length; i++) {
                            String part = fieldParts[i];
                            if (currentSchema.has(part)) {
                                if (i == fieldParts.length - 1) {
                                    JSONObject fieldSchema = currentSchema.getJSONObject(part);
                                    usage.setType(schemaTypeResolver.determineSchemaType(fieldSchema));
                                    return;
                                } else {
                                    currentSchema = currentSchema.getJSONObject(part);
                                    if (currentSchema.has("properties")) {
                                        currentSchema = currentSchema.getJSONObject("properties");
                                    } else {
                                        // Path doesn't fully exist in schema
                                        break;
                                    }
                                }
                            } else {
                                // Path doesn't exist in schema
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse schema for node key {}: {}", nodeKey, e.getMessage());
            }
        } else {
            // This could be a built-in node or a plugin node for which schema is not found.
            log.warn("No schema found for node key {}, skipping type generation for consumer field {}", nodeKey, key);
        }

        // If type was not determined, set it to "any"
        if (usage.getType() == null) {
            usage.setType("any");
        }
    }

    private String resolveAlias(String ref, Map<String, String> aliases) {
        if (aliases.containsKey(ref)) {
            return templateService.extractRefs(aliases.get(ref)).stream().findFirst().orElse(ref);
        }
        return ref;
    }
}