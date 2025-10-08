package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.loop.while_loop;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "core:flow.loop.while",
        name = "While Loop",
        version = "1.0.0",
        description = "Executes a loop while a specified condition is true. Supports break and continue conditions.",
        icon = "loop",
        type = "flow.loop",
        tags = {"flow", "loop", "while", "conditional"}
)
public class WhileLoopNode implements NodeDefinitionProvider {
    private final WhileLoopExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
    }
}
