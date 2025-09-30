package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.branch.switch_node;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "core:flow.branch.switch",
        name = "Switch Branch",
        version = "1.0.0",
        description = "Executes a branch based on the value of an expression. If no case matches, it uses a default case if provided.",
        type = "branch",
        tags = {"core", "flow", "branch", "switch"},
        icon = "ph:git-branch"
)
public class SwitchNode implements NodeDefinitionProvider {
    private final SwitchNodeExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
    }
}
