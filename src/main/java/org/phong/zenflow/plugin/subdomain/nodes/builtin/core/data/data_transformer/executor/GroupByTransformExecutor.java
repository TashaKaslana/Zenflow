package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.executor;

import org.springframework.stereotype.Component;

@Component
public class GroupByTransformExecutor extends AbstractSingleTransformExecutor {
    public GroupByTransformExecutor(DataTransformerExecutor delegate) {
        super(delegate);
    }

    @Override
    public String key() {
        return "core:data.transformer.group_by:1.0.0";
    }

    @Override
    protected String transformerName() {
        return "group_by";
    }
}
