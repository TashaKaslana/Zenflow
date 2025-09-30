package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.loop.for_each;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "core:flow.loop.for_each",
        name = "For Each Loop",
        version = "1.0.0",
        description = "Iterates over a collection of items and executes a sub-workflow for each item.",
        type = "flow",
        tags = {"core", "flow", "loop", "for-each"},
        icon = "ph:repeat"
)
public class ForEachLoopNode implements NodeDefinitionProvider {
    private final ForEachLoopExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
    }
}
