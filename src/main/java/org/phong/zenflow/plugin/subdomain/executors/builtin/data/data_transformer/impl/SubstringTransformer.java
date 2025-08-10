package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.impl;

import org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.interfaces.DataTransformer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SubstringTransformer implements DataTransformer {
    @Override
    public String getName() { return "substring"; }

    @Override
    public Object transform(Object input, Map<String, Object> params) {
        if (input == null) {
            return null;
        }
        String strInput = input.toString();
        int start = (int) params.getOrDefault("start", 0);
        int end = (int) params.getOrDefault("end", strInput.length());
        return strInput.substring(start, end);
    }
}
