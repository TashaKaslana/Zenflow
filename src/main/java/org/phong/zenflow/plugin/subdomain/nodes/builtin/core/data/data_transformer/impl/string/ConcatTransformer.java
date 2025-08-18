package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.impl.string;

import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.exception.DataTransformerExecutorException;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.interfaces.DataTransformer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ConcatTransformer implements DataTransformer {
    @Override
    public String getName() {
        return "concat";
    }

    @Override
    public Object transform(Object data, Map<String, Object> params) {
        if (params == null || !params.containsKey("suffix")) {
            throw new DataTransformerExecutorException("Suffix parameter is required for concat transformer.");
        }
        String suffix = String.valueOf(params.get("suffix"));
        return data + suffix;
    }
}
