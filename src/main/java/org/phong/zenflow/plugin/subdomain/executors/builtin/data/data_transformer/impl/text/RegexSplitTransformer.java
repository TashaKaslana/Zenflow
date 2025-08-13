package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.impl.text;

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
    public Object transform(Object data, Map<String, Object> params) {
        if (data == null) {
            return null;
        }
        String regex = (String) params.get("pattern");
        if (regex == null) {
            throw new IllegalArgumentException("Pattern parameter is required for regex_split transformer.");
        }
        return Arrays.asList(data.toString().split(regex));
    }
}
