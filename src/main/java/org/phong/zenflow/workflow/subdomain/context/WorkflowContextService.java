package org.phong.zenflow.workflow.subdomain.context;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.utils.TemplateEngine;
import org.phong.zenflow.plugin.subdomain.node.utils.SchemaRegistry;
import org.phong.zenflow.plugin.subdomain.node.utils.SchemaTemplateStringGenerator;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.OutputUsage;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowMetadata;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginDefinition;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Static context service for workflow nodes.
 * <p>
 * This service builds a static context for workflow nodes, resolving dependencies and consumer relationships
 * based on the node configurations and existing metadata.
 * </p>
 * <p>It also handles alias resolution and type inference.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WorkflowContextService {
    private final SchemaRegistry schemaRegistry;

    private static void generateAliasLinkBackToConsumers(WorkflowMetadata ctx) {
        for (Map.Entry<String, String> aliasEntry : ctx.alias().entrySet()) {
            String aliasName = aliasEntry.getKey();
            TemplateEngine.extractRefs(aliasEntry.getValue()).stream().findFirst().ifPresent(originalRef -> ctx.nodeConsumer()
                    .computeIfAbsent(originalRef, k -> new OutputUsage())
                    .getAlias()
                    .add(aliasName));
        }
    }

    public WorkflowMetadata buildStaticContext(List<BaseWorkflowNode> existingNodes, WorkflowMetadata existingMetadata) {
        WorkflowMetadata ctx = new WorkflowMetadata();

        // Preserve existing aliases from metadata
        if (existingMetadata != null && existingMetadata.alias() != null) {
            ctx.alias().putAll(ObjectConversion.safeConvert(existingMetadata.alias(), new TypeReference<>() {
            }));
        }

        generateDependenciesAndConsumers(existingNodes, ctx);
        generateTypeForConsumerFields(existingNodes, ctx);
        generateAliasLinkBackToConsumers(ctx);

        return new WorkflowMetadata(ctx.alias(), ctx.nodeDependency(), ctx.nodeConsumer());
    }

    private void generateDependenciesAndConsumers(List<BaseWorkflowNode> existingNodes, WorkflowMetadata ctx) {
        for (BaseWorkflowNode node : existingNodes) {
            String nodeKey = node.getKey();
            Map<String, Object> input = node.getConfig().input();
            if (input == null || input.isEmpty()) {
                continue;
            }

            for (Map.Entry<String, Object> entry : input.entrySet()) {
                Object inputValue = entry.getValue();
                if (inputValue == null) {
                    continue; // Skip null inputs
                }

                Set<String> referenced = TemplateEngine.extractRefs(inputValue.toString());

                for (String ref : referenced) {
                    String resolvedRef = resolveAlias(ref, ctx.alias());

                    // Add dependency
                    ctx.nodeDependency()
                            .computeIfAbsent(nodeKey, k -> new HashSet<>())
                            .add(resolvedRef);

                    // Track consumer
                    ctx.nodeConsumer()
                            .computeIfAbsent(resolvedRef, k -> new OutputUsage())
                            .getConsumers()
                            .add(nodeKey);
                }
            }
        }
    }

    private void generateTypeForConsumerFields(List<BaseWorkflowNode> existingNodes, WorkflowMetadata ctx) {
        List<String> templateStrings = SchemaTemplateStringGenerator.generateTemplateStrings(existingNodes);
        Map<String, JSONObject> schemas = schemaRegistry.getSchemaMapByTemplateStrings(templateStrings);

        for (BaseWorkflowNode node : existingNodes) {
            try {
                if (node instanceof PluginDefinition pluginNode) {
                    JSONObject schema = schemas.get(pluginNode.getPluginNode().nodeId().toString());
                    if (schema != null) {
                        populateOutputTypesFromSchema(node, schema, ctx);
                    } else {
                        log.warn("No schema found for plugin node {}", pluginNode.getPluginNode().nodeId());
                    }
                } else {
                    log.warn("Node {} is not a plugin node, skipping schema processing", node.getKey());
                }
            } catch (Exception e) {
                log.warn("Failed to load schema for node {}: {}", node.getKey(), e.getMessage());
            }
        }
    }

    private String resolveAlias(String ref, Map<String, String> aliases) {
        if (aliases.containsKey(ref)) {
            return TemplateEngine.extractRefs(aliases.get(ref)).stream().findFirst().orElse(ref);
        }
        return ref;
    }

    private void populateOutputTypesFromSchema(BaseWorkflowNode node, JSONObject schema, WorkflowMetadata ctx) {
        try {
            // Look for the output schema definition in the schema
            if (schema.has("properties") && schema.getJSONObject("properties").has("output")) {
                JSONObject outputSchema = schema.getJSONObject("properties").getJSONObject("output");

                // Get output schema properties if available
                if (outputSchema.has("properties")) {
                    JSONObject outputProperties = outputSchema.getJSONObject("properties");

                    // Process each output field defined in the schema
                    for (String key : outputProperties.keySet()) {
                        String outputKey = node.getKey() + ".output." + key;
                        // This will create a new OutputUsage object for each potential output defined in the schema.
                        OutputUsage outputUsage = ctx.nodeConsumer().computeIfAbsent(outputKey, k -> new OutputUsage());

                        JSONObject fieldSchema = outputProperties.getJSONObject(key);
                        outputUsage.setType(determineSchemaType(fieldSchema));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse output schema for node {}: {}", node.getKey(), e.getMessage());
        }
    }

    /**
     * Determine the type from a JSON schema object
     *
     * @param schema The schema object for a field
     * @return The determined type as a string
     */
    private String determineSchemaType(JSONObject schema) {
        if (schema.has("type")) {
            String type = schema.getString("type");
            return switch (type) {
                case "string" -> "string";
                case "number", "integer" -> "number";
                case "boolean" -> "boolean";
                case "array" -> {
                    // For arrays, try to determine item type
                    if (schema.has("items") && schema.getJSONObject("items").has("type")) {
                        yield "array:" + determineSchemaType(schema.getJSONObject("items"));
                    }
                    yield "array";
                }
                case "object" ->
                    // For objects, we might want to provide more specific info in the future
                        "object";
                default -> type;
            };
        }

        // Handle special cases like oneOf, anyOf, etc.
        if (schema.has("oneOf") || schema.has("anyOf") || schema.has("allOf")) {
            return "mixed";
        }

        return "unknown";
    }
}