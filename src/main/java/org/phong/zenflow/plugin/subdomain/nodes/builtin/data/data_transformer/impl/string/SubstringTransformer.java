package org.phong.zenflow.plugin.subdomain.nodes.builtin.data.data_transformer.impl.string;

import org.phong.zenflow.plugin.subdomain.nodes.builtin.data.data_transformer.interfaces.DataTransformer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SubstringTransformer implements DataTransformer {
    @Override
    public String getName() { return "substring"; }

    @Override
    public Object transform(Object data, Map<String, Object> params) {
        if (data == null) {
            return null;
        }
        String strInput = data.toString();
        int start = (int) params.getOrDefault("start", 0);
        int end = (int) params.getOrDefault("end", strInput.length());
        return strInput.substring(start, end);
    }
}
