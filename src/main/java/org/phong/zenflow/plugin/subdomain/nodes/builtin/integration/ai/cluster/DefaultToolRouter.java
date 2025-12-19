package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiToolRegistry;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Default ToolRouter implementation.
 * - Starts with the platform tool registry.
 * - Scans for child nodes to expose as tools (Node-as-Tool).
 */
@RequiredArgsConstructor
@Slf4j
public class DefaultToolRouter implements ToolRouter {

    private final AiToolRegistry baseRegistry;
    private final ToolRouterConfig config;
    private final ExecutionContext context;
    private final ObjectMapper objectMapper;

    @Override
    public List<Object> resolveTools() {
        if (config != null && !config.isEnabled()) {
            log.info("Tool routing disabled by configuration");
            return List.of();
        }
        
        List<Object> tools = new ArrayList<>(baseRegistry.getToolList());

        // Custom bean loading is intentionally omitted
        if (config != null && config.getCustomTools() != null && !config.getCustomTools().isEmpty()) {
            log.warn("custom_tools configured but ApplicationContext loading is disabled; ignoring custom_tools");
        }

        // Node-as-Tool Discovery
        if (context != null) {
            discoverNodeTools(tools);
        }

        return tools;
    }

    private void discoverNodeTools(List<Object> tools) {
        try {
            String currentKey = context.getNodeKey();
            BaseWorkflowNode clusterNode = context.getWorkflowNode(currentKey);
            
            if (clusterNode != null && clusterNode.getChildNodeKeys() != null) {
                for (String childKey : clusterNode.getChildNodeKeys()) {
                    // We wrap all children as tools. 
                    // The provider node is also a child, but usually the LLM won't call it unless instructed.
                    // Future improvement: Filter out the active provider node.
                    
                    BaseWorkflowNode childNode = context.getWorkflowNode(childKey);
                    if (childNode != null) {
                        log.debug("Registering node as tool: {}", childKey);
                        tools.add(new NodeToolCallback(childNode, context, objectMapper));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to discover node tools: {}", e.getMessage());
        }
    }
}
