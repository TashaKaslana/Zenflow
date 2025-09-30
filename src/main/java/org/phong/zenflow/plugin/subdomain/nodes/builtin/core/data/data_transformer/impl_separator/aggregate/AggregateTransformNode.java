package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.impl_separator.aggregate;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "core:data.transformer.aggregate",
        name = "Aggregate",
        version = "1.0.0",
        description = "Aggregates a list of records without grouping.",
        icon = "ph:sigma",
        tags = {"data", "aggregate"},
        type = "data_transformation"
)
public class AggregateTransformNode implements NodeDefinitionProvider {
    private final AggregateTransformExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
    }
}
