package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeStateType;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiToolRegistry;
import org.phong.zenflow.plugin.subdomain.execution.registry.PluginNodeExecutorRegistry;
import org.phong.zenflow.plugin.subdomain.schema.services.SchemaRegistry;
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
    private final SchemaRegistry schemaRegistry;
    private final PluginNodeExecutorRegistry pluginNodeExecutorRegistry;

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
                        if (!isClusterTool(childNode)) {
                            log.debug("Skipping internal node: {}", childKey);
                            continue;
                        }
                        log.debug("Registering node as tool: {}", childKey);
                        tools.add(new NodeToolCallback(childNode, context, objectMapper, schemaRegistry));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to discover node tools: {}", e.getMessage());
        }
    }

    private boolean isClusterTool(BaseWorkflowNode node) {
        if (pluginNodeExecutorRegistry == null) return false;
        
        try {
            String nodeId = null;
            if (node.getPluginNode() != null) {
                if (node.getPluginNode().getNodeId() != null) {
                    nodeId = node.getPluginNode().getNodeId().toString();
                } else {
                    // Try composite key
                    String compositeKey = node.getPluginNode().getPluginKey() + ":" + 
                                          node.getPluginNode().getNodeKey() + ":" + 
                                          node.getPluginNode().getVersion();
                    nodeId = pluginNodeExecutorRegistry.getIdByCompositeKey(compositeKey).orElse(null);
                }
            }
            
            if (nodeId != null) {
                return pluginNodeExecutorRegistry.getDefinition(nodeId)
                        .map(def -> NodeStateType.isAITool(def.getNodeState().stateTypes()))
                        .orElse(false);
            }
        } catch (Exception e) {
            log.warn("Failed to check if node is internal: {}", e.getMessage());
        }
        return false;
    }
}
