package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.executor;

import org.springframework.stereotype.Component;

@Component
public class AggregateTransformExecutor extends AbstractSingleTransformExecutor {
    public AggregateTransformExecutor(DataTransformerExecutor delegate) {
        super(delegate);
    }

    @Override
    public String key() {
        return "core:data.transformer.aggregate:1.0.0";
    }

    @Override
    protected String transformerName() {
        return "aggregate";
    }
}
