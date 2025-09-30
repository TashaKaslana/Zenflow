package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.discord.trigger;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "discord:message.trigger",
        name = "Discord Message Trigger",
        version = "1.0.0",
        description = "Listens for Discord messages and triggers workflows. Uses centralized hub for O(1) performance.",
        type = "trigger",
        triggerType = "event",
        tags = {"integration", "discord", "trigger", "message"},
        icon = "simple-icons:discord"
)
public class DiscordMessageTriggerNode implements NodeDefinitionProvider {
    private final DiscordMessageTriggerExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
    }
}
