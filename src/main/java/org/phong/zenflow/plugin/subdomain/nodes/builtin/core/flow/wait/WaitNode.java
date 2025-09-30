package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.wait;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "core:wait",
        name = "Wait",
        version = "1.0.0",
        description = "Waits for specified nodes to reach a certain state before proceeding.",
        type = "flow.wait",
        tags = {"core", "flow", "wait"},
        icon = "wait"
)
public class WaitNode implements NodeDefinitionProvider {
    private final WaitExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
    }
}
