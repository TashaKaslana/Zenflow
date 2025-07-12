package org.phong.zenflow.plugin.subdomain.executor.builtin.data_transformer.interfaces;

import java.util.Map;

public interface DataTransformer {
    String getName();
    String transform(String input, Map<String, Object> params);
}
