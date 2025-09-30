package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.loop.for_loop;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "core:flow.loop.for",
        name = "For Loop",
        version = "1.0.0",
        description = "Executes a loop with a defined start, end, and increment. Supports break and continue conditions.",
        type = "flow.loop",
        tags = {"loop", "flow", "iteration"},
        icon = "ph:repeat"
)
public class ForLoopNode implements NodeDefinitionProvider {
    private final ForLoopExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
    }
}
