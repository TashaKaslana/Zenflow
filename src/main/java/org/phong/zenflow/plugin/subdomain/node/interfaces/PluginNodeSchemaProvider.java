package org.phong.zenflow.plugin.subdomain.node.interfaces;

import java.util.List;
import java.util.Map;

public interface PluginNodeSchemaProvider {
    Map<String, Object> getSchemaJson(String key);
    Map<String, Map<String, Object>> getAllSchemasByIdentifiers(List<String> keys);
}
