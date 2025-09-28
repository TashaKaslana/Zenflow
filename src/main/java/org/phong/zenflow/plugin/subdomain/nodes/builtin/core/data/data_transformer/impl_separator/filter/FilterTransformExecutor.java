package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.impl_separator.filter;

import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.executor.AbstractSingleTransformExecutor;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.executor.DataTransformerExecutor;
import org.springframework.stereotype.Component;

@Component
@PluginNode(
        key = "core:data.transformer.filter",
        name = "Filter",
        version = "1.0.0",
        description = "Filters a list of records using an expression.",
        icon = "ph:funnel",
        tags = {"data", "filter"}
)
public class FilterTransformExecutor extends AbstractSingleTransformExecutor {
    public FilterTransformExecutor(DataTransformerExecutor delegate) {
        super(delegate);
    }
    @Override
    protected String transformerName() {
        return "filter";
    }
}
