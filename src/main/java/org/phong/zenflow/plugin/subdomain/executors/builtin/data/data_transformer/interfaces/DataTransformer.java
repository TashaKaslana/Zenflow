package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.interfaces;

import java.util.Map;

public interface DataTransformer {
    String getName();

    Object transform(Object input, Map<String, Object> params);
}
