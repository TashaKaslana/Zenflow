package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.openrouter.chat;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeState;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeStateType;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * OpenRouter chat node using the OpenAI-compatible Chat Completions API.
 */
@Component
@AllArgsConstructor
@PluginNode(
        key = "openrouter:chat",
        name = "OpenRouter Chat",
        version = "1.0.0",
        description = "Execute OpenRouter chat completions via the OpenRouter API proxy.",
        type = "integration.ai",
        tags = {"integration", "ai", "openrouter", "llm"},
        icon = "mdi:router-network",
        schemaPath = "schema.json"
)
public class OpenRouterNode implements NodeDefinitionProvider {

    private final OpenRouterExecutor executor;
    private final OpenRouterResourceManager resourceManager;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .nodeResourceManager(resourceManager)
                .nodeState(new NodeState(Set.of(
                        NodeStateType.AI_PROVIDER,
                        NodeStateType.NORMAL
                )))
                .build();
    }
}
