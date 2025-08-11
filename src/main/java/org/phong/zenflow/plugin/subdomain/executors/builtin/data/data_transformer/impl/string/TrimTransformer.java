package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.impl.string;

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
    public Object transform(Object data, Map<String, Object> params) {
        return data == null ? null : data.toString().trim();
    }
}
