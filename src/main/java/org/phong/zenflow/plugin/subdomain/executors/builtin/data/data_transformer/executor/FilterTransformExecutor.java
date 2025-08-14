package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.executor;

import org.springframework.stereotype.Component;

@Component
public class FilterTransformExecutor extends AbstractSingleTransformExecutor {
    public FilterTransformExecutor(DataTransformerExecutor delegate) {
        super(delegate);
    }

    @Override
    public String key() {
        return "core:data.transformer.filter:1.0.0";
    }

    @Override
    protected String transformerName() {
        return "filter";
    }
}
