package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.ai.gemini;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

/**
 * Gemini AI node for Google Vertex AI
 */
@Component
@AllArgsConstructor
@PluginNode(
        key = "google-ai:gemini",
        name = "Gemini AI",
        version = "1.0.0",
        description = "Execute Gemini AI model requests with support for text and JSON responses via Google Vertex AI",
        type = "integration.ai",
        tags = {"integration", "ai", "gemini", "llm", "google", "vertex-ai"},
        icon = "simple-icons:google",
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
