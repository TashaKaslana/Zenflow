package org.phong.zenflow.plugin.subdomain.schema.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.phong.zenflow.plugin.subdomain.node.exception.PluginNodeException;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.repository.PluginNodeRepository;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Service responsible for generating sample configuration data for a plugin node
 * based on its JSON schema definition.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NodeSampleDataService {

    private final SchemaRegistry schemaRegistry;
    private final PluginNodeRepository pluginNodeRepository;
    private final Random random = new Random();

    /**
     * Generate sample configuration data for the given plugin node.
     *
     * @param nodeId id of the plugin node
     * @return map representing sample configuration
     */
    public Map<String, Object> getSampleData(UUID nodeId) {
        PluginNode node = pluginNodeRepository.findById(nodeId)
                .orElseThrow(() -> new PluginNodeException("PluginNode not found with id: " + nodeId));

        PluginNodeIdentifier identifier = new PluginNodeIdentifier(
                node.getPlugin().getKey(),
                node.getKey(),
                node.getPluginNodeVersion(),
                node.getExecutorType()
        );

        JSONObject schema = schemaRegistry.getSchemaByTemplateString(identifier.toCacheKey());
        Object sample = generateSample(schema);
        if (sample instanceof Map<?, ?> map) {
            //noinspection unchecked
            return (Map<String, Object>) map;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("value", sample);
        return result;
    }

    private Object generateSample(JSONObject schema) {
        String type = schema.optString("type");
        if ("object".equals(type)) {
            JSONObject props = schema.optJSONObject("properties");
            Map<String, Object> result = new LinkedHashMap<>();
            if (props != null) {
                for (String key : props.keySet()) {
                    result.put(key, generateSample(props.getJSONObject(key)));
                }
            }
            return result;
        }
        if ("array".equals(type)) {
            List<Object> list = new ArrayList<>();
            JSONObject items = schema.optJSONObject("items");
            int minItems = schema.optInt("minItems", 1);
            for (int i = 0; i < Math.max(1, minItems); i++) {
                if (items != null) {
                    list.add(generateSample(items));
                }
            }
            return list;
        }
        if ("string".equals(type)) {
            if (schema.has("enum")) {
                JSONArray arr = schema.getJSONArray("enum");
                return arr.isEmpty() ? "" : arr.get(0);
            }
            String format = schema.optString("format");
            return switch (format) {
                case "uuid" -> UUID.randomUUID().toString();
                case "date-time" -> Instant.now().toString();
                default -> "sample";
            };
        }
        if ("integer".equals(type)) {
            int min = schema.optInt("minimum", 0);
            int max = schema.optInt("maximum", min + 10);
            return min + random.nextInt(Math.max(1, max - min + 1));
        }
        if ("number".equals(type)) {
            double min = schema.has("minimum") ? schema.getDouble("minimum") : 0d;
            double max = schema.has("maximum") ? schema.getDouble("maximum") : min + 10d;
            return min + (max - min) * random.nextDouble();
        }
        if ("boolean".equals(type)) {
            return true;
        }
        // default fall-back
        return null;
    }
}
