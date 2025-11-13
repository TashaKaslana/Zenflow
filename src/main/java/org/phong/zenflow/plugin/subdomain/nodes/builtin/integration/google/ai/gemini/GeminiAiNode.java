package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.ai.gemini;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

/**
 * Default Gemini AI node that talks to the Gemini API via the native Gemini client.
 */
@Component
@AllArgsConstructor
@PluginNode(
        key = "google-ai:gemini",
        name = "Gemini AI",
        version = "1.0.0",
        description = "Execute Gemini API requests directly via Google's Gemini endpoint using host + API key credentials",
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
