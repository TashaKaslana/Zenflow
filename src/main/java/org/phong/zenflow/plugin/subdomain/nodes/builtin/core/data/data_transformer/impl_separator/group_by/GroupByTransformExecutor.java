package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.impl_separator.group_by;

import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.executor.AbstractSingleTransformExecutor;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.executor.DataTransformerExecutor;
import org.springframework.stereotype.Component;

@Component
public class GroupByTransformExecutor extends AbstractSingleTransformExecutor {
    public GroupByTransformExecutor(DataTransformerExecutor delegate) {
        super(delegate);
    }
    @Override
    protected String transformerName() {
        return "group_by";
    }
}
