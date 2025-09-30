package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.test.data_generator;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "test:data.generate",
        name = "Data Generator",
        version = "1.0.0"
)
public class DataGeneratorNode implements NodeDefinitionProvider {
    private final DataGeneratorExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
    }
}
