package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.impl;

import org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.interfaces.DataTransformer;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

@Component
public class RegexSplitTransformer implements DataTransformer {
    @Override
    public String getName() {
        return "regex_split";
    }

    @Override
    public Object transform(Object input, Map<String, Object> params) {
        if (input == null) {
            return null;
        }
        String regex = (String) params.get("pattern");
        if (regex == null) {
            throw new IllegalArgumentException("Pattern parameter is required for regex_split transformer.");
        }
        return Arrays.asList(input.toString().split(regex));
    }
}
