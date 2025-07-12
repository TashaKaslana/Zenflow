package org.phong.zenflow.plugin.subdomain.executors.builtin.data_transformer.impl;

import org.phong.zenflow.plugin.subdomain.executors.builtin.data_transformer.exception.DataTransformerExecutorException;
import org.phong.zenflow.plugin.subdomain.executors.builtin.data_transformer.interfaces.DataTransformer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ConcatTransformer implements DataTransformer {
    @Override
    public String getName() {
        return "concat";
    }

    @Override
    public String transform(String input, Map<String, Object> params) {
        if (params == null || !params.containsKey("suffix")) {
            throw new DataTransformerExecutorException("Suffix parameter is required for concat transformer.");
        }
        String suffix = (String) params.get("suffix");
        return input + suffix;
    }
}
