package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.impl_separator.aggregate;

import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.executor.AbstractSingleTransformExecutor;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.executor.DataTransformerExecutor;
import org.springframework.stereotype.Component;

@Component
@PluginNode(
        key = "core:data.transformer.aggregate",
        name = "Aggregate",
        version = "1.0.0",
        description = "Aggregates a list of records without grouping.",
        icon = "ph:sigma",
        tags = {"data", "aggregate"},
        type = "data_transformation"
)
public class AggregateTransformExecutor extends AbstractSingleTransformExecutor {
    public AggregateTransformExecutor(DataTransformerExecutor delegate) {
        super(delegate);
    }
    @Override
    protected String transformerName() {
        return "aggregate";
    }
}
