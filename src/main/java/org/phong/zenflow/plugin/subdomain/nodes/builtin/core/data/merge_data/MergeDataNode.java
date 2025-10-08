package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.merge_data;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "core:merge_data",
        name = "Merge Data",
        version = "1.0.0",
        description = "Merges multiple data sources into a single output based on specified strategies.",
        type = "data_transformation",
        tags = {"data", "merge", "transformation"},
        icon = "merge_data"
)
public class MergeDataNode implements NodeDefinitionProvider {
    private final MergeDataExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
    }
}
