package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.impl_separator.filter;

import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.executor.AbstractSingleTransformExecutor;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.executor.DataTransformerExecutor;
import org.springframework.stereotype.Component;

@Component
public class FilterTransformExecutor extends AbstractSingleTransformExecutor {
    public FilterTransformExecutor(DataTransformerExecutor delegate) {
        super(delegate);
    }
    @Override
    protected String transformerName() {
        return "filter";
    }
}
