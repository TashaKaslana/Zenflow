package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.ai.gemini;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

/**
 * Gemini AI node backed by Spring AI Google GenAI (API key or Vertex AI).
 */
@Component
@AllArgsConstructor
@PluginNode(
        key = "google-ai:gemini",
        name = "Gemini AI",
        version = "1.0.0",
        description = "Execute Gemini requests via Spring AI Google GenAI using API key or Vertex AI credentials",
        type = "integration.ai",
        tags = {"integration", "ai", "gemini", "llm"},
        icon = "simple-icons:googlecolab",
        schemaPath = "schema.json"
)
public class GeminiAiNode implements NodeDefinitionProvider {

    private final GeminiAiExecutor executor;
    private final GeminiResourceManager resourceManager;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .nodeResourceManager(resourceManager)
                .build();
    }
}
