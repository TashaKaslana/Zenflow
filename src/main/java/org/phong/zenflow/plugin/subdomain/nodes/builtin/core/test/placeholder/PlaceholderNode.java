package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.test.placeholder;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "test:placeholder",
        name = "Placeholder Node",
        version = "1.0.0",
        description = "A placeholder node that echoes its input as output.",
        icon = "ph:placeholder",
        tags = {"data", "placeholder"},
        type = "data"
)
public class PlaceholderNode implements NodeDefinitionProvider {
    private final PlaceholderExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
    }
}
