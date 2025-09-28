package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.impl_separator.group_by;

import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.executor.AbstractSingleTransformExecutor;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.executor.DataTransformerExecutor;
import org.springframework.stereotype.Component;

@Component
@PluginNode(
        key = "core:data.transformer.group_by",
        name = "Group By",
        version = "1.0.0",
        description = "Groups records and applies aggregations.",
        icon = "ph:columns",
        tags = {"data", "group", "aggregate"}
)
public class GroupByTransformExecutor extends AbstractSingleTransformExecutor {
    public GroupByTransformExecutor(DataTransformerExecutor delegate) {
        super(delegate);
    }
    @Override
    protected String transformerName() {
        return "group_by";
    }
}
