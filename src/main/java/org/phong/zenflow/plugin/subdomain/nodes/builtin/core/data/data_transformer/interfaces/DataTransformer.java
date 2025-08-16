package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.interfaces;

import java.util.Map;

public interface DataTransformer {
    String getName();

    Object transform(Object data, Map<String, Object> params);
}
