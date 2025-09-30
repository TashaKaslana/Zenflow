package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.impl_separator.group_by;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "core:data.transformer.group_by",
        name = "Group By",
        version = "1.0.0",
        description = "Groups records and applies aggregations.",
        icon = "ph:columns",
        tags = {"data", "group", "aggregate"}
)
public class GroupByTransformNode implements NodeDefinitionProvider {
    private final GroupByTransformExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
    }
}
