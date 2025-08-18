package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.impl.field;

import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.exception.DataTransformerExecutorException;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.interfaces.DataTransformer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GetFieldTransformer implements DataTransformer {
    @Override
    public String getName() {
        return "get_field";
    }

    @Override
    public Object transform(Object data, Map<String, Object> params) {
        if (!(data instanceof Map<?, ?> map)) {
            throw new DataTransformerExecutorException("Input must be a Map for get_field transformer.");
        }
        if (params == null || !params.containsKey("field")) {
            throw new DataTransformerExecutorException("Field parameter is required for get_field transformer.");
        }

        String field = (String) params.get("field");
        return map.get(field);
    }
}
