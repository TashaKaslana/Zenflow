package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.impl.string;

import org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.interfaces.DataTransformer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class LowercaseTransformer implements DataTransformer {
    @Override
    public String getName() {
        return "to_lowercase";
    }

    @Override
    public Object transform(Object data, Map<String, Object> params) {
        if (data == null) {
            return null;
        }
        return data.toString().toLowerCase();
    }
}
