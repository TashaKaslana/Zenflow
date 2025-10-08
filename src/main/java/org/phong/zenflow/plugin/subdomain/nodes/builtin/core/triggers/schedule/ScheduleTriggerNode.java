package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.triggers.schedule;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "core:schedule.trigger",
        name = "Schedule Trigger",
        version = "1.0.0",
        description = "Triggers workflows using a shared Quartz Scheduler for maximum efficiency. " +
                "Supports both interval and cron-based scheduling with database persistence.",
        type = "trigger",
        triggerType = "schedule",
        tags = {"core", "trigger", "schedule", "quartz", "optimized"},
        icon = "ph:clock"
)
public class ScheduleTriggerNode implements NodeDefinitionProvider {
    private final ScheduleTriggerExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
    }
}
