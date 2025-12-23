package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.jspecify.annotations.NonNull;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.plugin.subdomain.schema.services.SchemaRegistry;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_definition.enums.NodeType;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter that exposes a Zenflow workflow node as a Spring AI Tool.
 */
@Slf4j
public class NodeToolCallback implements ToolCallback {

    private final BaseWorkflowNode node;
    private final ExecutionContext context;
    private final ObjectMapper objectMapper;
    private final SchemaRegistry schemaRegistry;
    private final ToolDefinition toolDefinition;

    private static final String DEFAULT_SCHEMA = """
                {
                    "type": "object",
                    "description": "Input parameters for the node",
                    "additionalProperties": true
                }
                """;

    public NodeToolCallback(BaseWorkflowNode node,
                            ExecutionContext context,
                            ObjectMapper objectMapper,
                            SchemaRegistry schemaRegistry) {
        this.node = node;
        this.context = context;
        this.objectMapper = objectMapper;
        this.schemaRegistry = schemaRegistry;
        this.toolDefinition = buildToolDefinition(node);
    }

    private ToolDefinition buildToolDefinition(BaseWorkflowNode node) {
        String name = node.getKey().replace(":", "_").replace("-", "_"); // Sanitize name
        String description = "Execute node: " + node.getKey();
        
        String inputSchema;
        try {
            // Determine schema key
            String schemaKey = null;
            if (node.getPluginNode() != null && node.getPluginNode().getNodeId() != null) {
                 schemaKey = node.getPluginNode().getNodeId().toString();
            } else if (node.getType() != NodeType.PLUGIN) {
                 schemaKey = "builtin:" + node.getType().getNodeType();
            }
            
            if (schemaKey != null && schemaRegistry != null) {
                JSONObject schemaJson = schemaRegistry.getSchemaByTemplateString(schemaKey);
                if (schemaJson != null) {
                    inputSchema = schemaJson.toString();
                } else {
                    inputSchema = DEFAULT_SCHEMA;
                }
            } else {
                inputSchema = DEFAULT_SCHEMA;
            }
        } catch (Exception e) {
            log.warn("Failed to load schema for node tool {}: {}", node.getKey(), e.getMessage());
            inputSchema = DEFAULT_SCHEMA;
        }

        return ToolDefinition.builder()
                .name(name)
                .description(description)
                .inputSchema(inputSchema)
                .build();
    }

    @Override
    public @NonNull ToolDefinition getToolDefinition() {
        return toolDefinition;
    }

    @Override
    public @NonNull String call(@NonNull String input) {
        log.info("Tool call triggered for node: {}", node.getKey());
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> inputMap = objectMapper.readValue(input, Map.class);

            WorkflowConfig mergedConfig = mergeConfig(node.getConfig(), inputMap);

            ExecutionResult result = context.executeSubNode(node, mergedConfig);
            
            if (result.getStatus() == ExecutionStatus.SUCCESS) {
                // Check for output payload (populated by executeSubNode from capture or direct return)
                if (result.getOutputPayload() != null) {
                    return objectMapper.writeValueAsString(result.getOutputPayload());
                }
                
                return objectMapper.writeValueAsString(Map.of("status", "success", "message", "Node executed successfully (no output captured)"));
            } else {
                return objectMapper.writeValueAsString(Map.of(
                        "status", "error",
                        "message", result.getError()
                ));
            }
            
        } catch (Exception e) {
            log.error("Failed to execute node tool: {}", node.getKey(), e);
            return "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}";
        }
    }

    private WorkflowConfig mergeConfig(WorkflowConfig baseConfig, Map<String, Object> overrides) {
        Map<String, Object> mergedInput = new HashMap<>();
        List<String> profileKeys = null;
        Map<String, Object> output = null;

        if (baseConfig != null) {
            if (baseConfig.input() != null) {
                mergedInput.putAll(baseConfig.input());
            }
            List<String> baseProfiles = baseConfig.profile();
            if (baseProfiles != null && !baseProfiles.isEmpty()) {
                profileKeys = new ArrayList<>(baseProfiles);
            }
            Map<String, Object> baseOutput = baseConfig.output();
            if (baseOutput != null && !baseOutput.isEmpty()) {
                output = new HashMap<>(baseOutput);
            }
        }

        if (overrides != null && !overrides.isEmpty()) {
            mergedInput.putAll(overrides);
        }

        return new WorkflowConfig(mergedInput, profileKeys, output);
    }
}
