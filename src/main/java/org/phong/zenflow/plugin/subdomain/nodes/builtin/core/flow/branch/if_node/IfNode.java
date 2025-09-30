package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.branch.if_node;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "core:flow.branch.if",
        name = "If Branch",
        version = "1.0.0",
        type = "branch",
        description = "Executes a branch based on a boolean condition. If the condition is true, it proceeds to the 'next_true' node; otherwise, it goes to 'next_false'.",
        tags = {"core", "flow", "branch", "if", "condition"},
        icon = "ph:git-fork"
)
public class IfNode implements NodeDefinitionProvider {
    private final IfNodeExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
    }
}
