package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.cluster;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

/**
 * AI Cluster node - orchestrates AI execution with memory, tools, and multi-model support.
 * Delegates to abstract provider nodes (Gemini, OpenAI, etc.)
 */
@Component
@AllArgsConstructor
@PluginNode(
        key = "core:ai.cluster",
        name = "AI Cluster",
        version = "1.0.0",
        description = "Orchestrate AI model execution with conversation memory, tool calling, and multi-model support",
        type = "integration.ai",
        tags = {"ai", "llm", "cluster", "orchestration", "memory", "tools"},
        icon = "mdi:robot",
        schemaPath = "schema.json"
)
public class AiClusterNode implements NodeDefinitionProvider {
    private final AiClusterExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
    }
}
