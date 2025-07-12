package org.phong.zenflow.plugin.subdomain.executors.builtin.data_transformer.impl;

import org.phong.zenflow.plugin.subdomain.executors.builtin.data_transformer.interfaces.DataTransformer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RegexSplitTransformer implements DataTransformer {
    @Override
    public String getName() {
        return "regex_split";
    }

    @Override
    public String transform(String input, Map<String, Object> params) {
        String regex = (String) params.get("pattern");
        String joinWith = (String) params.getOrDefault("join_with", "|");

        return String.join(joinWith, input.split(regex));
    }
}
