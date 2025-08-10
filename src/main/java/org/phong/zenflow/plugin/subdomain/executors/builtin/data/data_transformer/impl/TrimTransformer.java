package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.impl;

import org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.interfaces.DataTransformer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TrimTransformer implements DataTransformer {
    @Override
    public String getName() {
        return "trim";
    }

    @Override
    public Object transform(Object input, Map<String, Object> params) {
        return input == null ? null : input.toString().trim();
    }
}
