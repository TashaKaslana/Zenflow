package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.discord.executor;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.discord.core.DiscordJdaResourceManager;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "discord:message.send",
        name = "Discord Send Message",
        version = "1.0.0",
        description = "Sends a message to a Discord channel using JDA. Supports text messages and embeds.",
        type = "integration.message",
        tags = {"integration", "discord", "message", "send"},
        icon = "simple-icons:discord"
)
public class DiscordMessageNode implements NodeDefinitionProvider {
    private final DiscordMessageExecutor executor;
    private final DiscordJdaResourceManager resourceManager;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .nodeResourceManager(resourceManager)
                .build();
    }
}
