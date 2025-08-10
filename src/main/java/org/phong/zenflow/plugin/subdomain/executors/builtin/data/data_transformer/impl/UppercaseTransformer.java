package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.impl;

import org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.interfaces.DataTransformer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UppercaseTransformer implements DataTransformer {

    @Override
    public String getName() {
        return "uppercase";
    }

    @Override
    public String transform(Object input, Map<String, Object> params) {
        if (input == null) {
            return null;
        }

        return input.toString().toUpperCase();
    }
}
