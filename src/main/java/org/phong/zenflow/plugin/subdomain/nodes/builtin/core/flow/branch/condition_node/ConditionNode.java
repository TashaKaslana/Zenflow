package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.branch.condition_node;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "core:flow.branch.condition",
        name = "Condition Branch",
        version = "1.0.0",
        description = "Executes a branch based on conditions defined in the input. " +
                "Each case is evaluated in order, and the first matching case will be executed. " +
                "If no cases match, the default case will be executed if provided.",
        type = "flow",
        tags = {"core", "flow", "branch", "condition"},
        icon = "ph:git-branch"
)
public class ConditionNode implements NodeDefinitionProvider {
    private final ConditionNodeExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
    }
}
