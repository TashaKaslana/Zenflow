package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.impl;

import org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.interfaces.DataTransformer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class LowercaseTransformer implements DataTransformer {

    @Override
    public String getName() {
        return "lowercase";
    }

    @Override
    public String transform(String input, Map<String, Object> params) {
        return input.toLowerCase();
    }
}
