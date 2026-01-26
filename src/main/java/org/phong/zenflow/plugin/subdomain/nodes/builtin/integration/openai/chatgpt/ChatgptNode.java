package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.openai.chatgpt;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

/**
 * ChatGPT node backed by OpenAI's Chat Completions API.
 */
@Component
@AllArgsConstructor
@PluginNode(
        key = "openai:chatgpt",
        name = "ChatGPT",
        version = "1.0.0",
        description = "Execute OpenAI Chat Completions with API key credentials (or compatible hosts)",
        type = "integration.ai",
        tags = {"integration", "ai", "chatgpt", "llm"},
        icon = "simple-icons:openai",
        schemaPath = "schema.json"
)
public class ChatgptNode implements NodeDefinitionProvider {

    private final ChatgptExecutor executor;
    private final ChatgptResourceManager resourceManager;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .nodeResourceManager(resourceManager)
                .build();
    }
}
