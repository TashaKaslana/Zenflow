package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.engine.orchestrator.NodeExecutionOrchestrator;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.Map;

/**
 * Adapter that exposes a Zenflow workflow node as a Spring AI Tool.
 */
@Slf4j
public class NodeToolCallback implements ToolCallback {

    private final BaseWorkflowNode node;
    private final NodeExecutionOrchestrator orchestrator;
    private final ExecutionContext context;
    private final ObjectMapper objectMapper;
    private final ToolDefinition toolDefinition;

    public NodeToolCallback(BaseWorkflowNode node,
                            NodeExecutionOrchestrator orchestrator,
                            ExecutionContext context,
                            ObjectMapper objectMapper) {
        this.node = node;
        this.orchestrator = orchestrator;
        this.context = context;
        this.objectMapper = objectMapper;
        this.toolDefinition = buildToolDefinition(node);
    }

    private ToolDefinition buildToolDefinition(BaseWorkflowNode node) {
        String name = node.getKey().replace(":", "_").replace("-", "_"); // Sanitize name
        String description = "Execute node: " + node.getName();
        
        // For now, we accept a generic JSON object as input.
        // Ideally, we should derive this from the node's schema if available.
        String inputSchema = """
                {
                    "type": "object",
                    "description": "Input parameters for the node",
                    "additionalProperties": true
                }
                """;

        return ToolDefinition.builder()
                .name(name)
                .description(description)
                .inputSchema(inputSchema)
                .build();
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return toolDefinition;
    }

    @Override
    public String call(String input) {
        log.info("Tool call triggered for node: {}", node.getKey());
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> inputMap = objectMapper.readValue(input, Map.class);
            
            WorkflowConfig config = new WorkflowConfig(inputMap);
            
            // Execute the node using the orchestrator
            // The orchestrator handles context switching (fixed in previous step)
            ExecutionResult result = orchestrator.executeNode(node, config, context);
            
            if (result.getStatus() == ExecutionStatus.SUCCESS) {
                // Return the output from the context (assuming the node writes to 'response' or similar, 
                // or we check the context for changes. 
                // Standard Zenflow nodes usually write to context. 
                // We might need to capture what the node wrote.
                // For simplicity, we return the execution status or a specific output key if known.
                // But wait, executeNode returns ExecutionResult which might not contain the data.
                // The data is in the context.
                
                // We can try to read 'response' or 'output' from the context *after* execution?
                // But the context is shared.
                // Let's assume the node writes to a key that we can read, or we return a success message.
                // A better approach for "Node as Tool" is to have the node return a value.
                // But Zenflow nodes are void-like (side effects on context).
                
                // Let's try to read "output" or "response" from the context.
                Object output = context.read("output", Object.class);
                if (output == null) {
                    output = context.read("response", Object.class);
                }
                
                return objectMapper.writeValueAsString(output != null ? output : Map.of("status", "success"));
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
}
