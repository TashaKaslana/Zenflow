package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.triggers.polling;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "core:polling.trigger",
        name = "Polling Trigger",
        version = "1.0.0",
        description = "Polls an HTTP endpoint at regular intervals using Quartz scheduler and triggers workflows when changes are detected. " +
                "Uses generic resource management for efficient caching and cleanup.",
        type = "trigger",
        triggerType = "polling",
        tags = {"core", "trigger", "polling", "http", "schedule", "quartz"},
        icon = "ph:arrow-clockwise"
)
public class PollingTriggerNode implements NodeDefinitionProvider {
    private final PollingTriggerExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
    }
}
