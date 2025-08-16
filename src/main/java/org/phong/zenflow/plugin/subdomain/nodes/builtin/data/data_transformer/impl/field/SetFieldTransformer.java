package org.phong.zenflow.plugin.subdomain.nodes.builtin.data.data_transformer.impl.field;

import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.data.data_transformer.exception.DataTransformerExecutorException;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.data.data_transformer.interfaces.DataTransformer;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class SetFieldTransformer implements DataTransformer {
    @Override
    public String getName() {
        return "set_field";
    }

    @Override
    public Object transform(Object data, Map<String, Object> params) {
        if (!(data instanceof Map)) {
            throw new DataTransformerExecutorException("Input must be a Map for set_field transformer.");
        }
        if (params == null || !params.containsKey("field")) {
            throw new DataTransformerExecutorException("Field parameter is required for set_field transformer.");
        }
        if (!params.containsKey("value")) {
            throw new DataTransformerExecutorException("Value parameter is required for set_field transformer.");
        }

        Map<String, Object> newMap = new HashMap<>(ObjectConversion.convertObjectToMap(data));
        String field = (String) params.get("field");
        Object value = params.get("value");
        newMap.put(field, value);
        return newMap;
    }
}
