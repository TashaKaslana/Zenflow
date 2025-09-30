package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.triggers.manual;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "core:manual.trigger",
        name = "Manual Trigger",
        version = "1.0.0",
        description = "Executes a manual trigger with optional payload and schedule configuration.",
        type = "trigger",
        triggerType = "manual",
        tags = {"core", "trigger", "manual"},
        icon = "ph:play"
)
public class ManualTriggerNode implements NodeDefinitionProvider {
    private final ManualTriggerExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
    }
}
