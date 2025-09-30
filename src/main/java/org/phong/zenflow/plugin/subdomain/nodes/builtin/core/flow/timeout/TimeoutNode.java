package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.timeout;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "core:flow.timeout",
        name = "Timeout",
        version = "1.0.0",
        description = "Schedules a timeout for a workflow node execution.",
        type = "flow.timeout",
        tags = {"core", "flow", "timeout", "delay"},
        icon = "ph:clock"
)
public class TimeoutNode implements NodeDefinitionProvider {
    private final TimeoutExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
    }
}
