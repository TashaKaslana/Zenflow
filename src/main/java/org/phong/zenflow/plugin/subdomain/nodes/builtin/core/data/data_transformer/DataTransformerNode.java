package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.executor.DataTransformerExecutor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "core:data.transformer",
        name = "Data Transformer",
        version = "1.0.0",
        description = "Executes data transformation using registered transformers. Supports both single-transform and pipeline modes.",
        type = "data_transformation",
        tags = {"data", "transformation", "pipeline"},
        icon = "ph:code",
        schemaPath = "../schema.json",
        docPath = "../doc.md"
)
public class DataTransformerNode implements NodeDefinitionProvider {
    private final DataTransformerExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
    }
}
