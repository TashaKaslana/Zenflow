package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.ai.gemini;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

/**
 * Gemini AI node backed by Google Vertex AI (legacy implementation).
 */
@Component
@AllArgsConstructor
@PluginNode(
        key = "google-ai:gemini-vertex",
        name = "Gemini AI (Vertex)",
        version = "1.0.0",
        description = "Execute Gemini AI model requests via Google Vertex AI using GCP credentials",
        type = "integration.ai",
        tags = {"integration", "ai", "gemini", "llm", "google", "vertex-ai"},
        icon = "simple-icons:google",
        schemaPath = "schema.vertex.json"
)
public class GeminiVertexAiNode implements NodeDefinitionProvider {
    private final GeminiVertexAiExecutor executor;
    private final GeminiVertexResourceManager resourceManager;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .nodeResourceManager(resourceManager)
                .build();
    }
}
