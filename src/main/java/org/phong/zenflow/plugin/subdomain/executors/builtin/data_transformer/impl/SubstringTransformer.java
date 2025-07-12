package org.phong.zenflow.plugin.subdomain.executors.builtin.data_transformer.impl;

import org.phong.zenflow.plugin.subdomain.executors.builtin.data_transformer.interfaces.DataTransformer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SubstringTransformer implements DataTransformer {
    @Override
    public String getName() { return "substring"; }

    @Override
    public String transform(String input, Map<String, Object> params) {
        int start = (int) params.getOrDefault("start", 0);
        int end = (int) params.getOrDefault("end", input.length());
        return input.substring(start, end);
    }
}
