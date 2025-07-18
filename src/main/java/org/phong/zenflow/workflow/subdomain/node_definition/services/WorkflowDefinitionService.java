package org.phong.zenflow.workflow.subdomain.node_definition.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.phong.zenflow.plugin.subdomain.node.utils.JsonSchemaValidator;
import org.phong.zenflow.plugin.subdomain.node.utils.SchemaRegistry;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.enums.NodeType;
import org.phong.zenflow.workflow.subdomain.node_definition.exception.WorkflowNodeDefinitionException;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;
import org.phong.zenflow.workflow.utils.NodeKeyGenerator;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowDefinitionService {

    private final SchemaRegistry schemaRegistry;
    private final ObjectMapper objectMapper;

    /**
     * Upsert (update if exists, insert if not). Auto-generates key if missing.
     * Validates each node as it's being upserted.
     */
    public void upsertNodes(List<Map<String, Object>> nodes, List<Map<String, Object>> updates) {
        Map<String, Map<String, Object>> keyToNode = nodes.stream()
                .collect(Collectors.toMap(
                        node -> (String) node.get("key"),
                        node -> node,
                        (existing, replacement) -> existing
                ));

        for (Map<String, Object> update : updates) {
            String type = (String) update.get("type");
            if (type == null || type.isBlank()) {
                throw new WorkflowNodeDefinitionException("Each node must have a 'type'");
            }

            String key = (String) update.get("key");
            if (key == null || key.isBlank()) {
                key = NodeKeyGenerator.generateKey(type);
                update.put("key", key);
            }

            // Validate the node before adding it to the workflow
            validateNode(update);
            keyToNode.put(key, update);
        }

        // Replace full list
        nodes.clear();
        nodes.addAll(keyToNode.values());
    }

    /**
     * Validates a single node's schema
     *
     * @param nodeMap The node to validate
     * @throws WorkflowNodeDefinitionException if validation fails
     */
    private void validateNode(Map<String, Object> nodeMap) {
        try {
            String type = (String) nodeMap.get("type");
            log.info("Validating node type '{}'", type);

            if (type.equalsIgnoreCase(NodeType.TRIGGER.name())) {
                String triggerType = (String) nodeMap.get("triggerType");

                boolean isValidTrigger = triggerType != null &&
                        Arrays.stream(TriggerType.values())
                                .map(Enum::name)
                                .anyMatch(name -> name.equalsIgnoreCase(triggerType));

                if (!isValidTrigger) {
                    throw new WorkflowNodeDefinitionException(
                            "Trigger nodes must have a valid 'triggerType' that matches one of the known types: " +
                                    Arrays.toString(TriggerType.values()));
                }
                return;
            }

            if (!type.equalsIgnoreCase(NodeType.PLUGIN.name())) {
                log.debug("Skipping validation for non-plugin node type: {}", type);
                return;
            }

            // Convert raw map to BaseWorkflowNode for validation
            BaseWorkflowNode nodeSchema = objectMapper.convertValue(nodeMap, BaseWorkflowNode.class);

            log.debug("Validating node schema for key: {}, config: {}", nodeSchema.getKey(), nodeSchema.getConfig());
            JSONObject config = new JSONObject(nodeSchema.getConfig());
            JSONObject nodeConfigSchema;

            if (nodeSchema instanceof PluginDefinition pluginDefinition) {
                PluginNodeDefinition pluginNodeDefinition = pluginDefinition.getPluginNode();
                nodeConfigSchema = schemaRegistry.getPluginSchema(
                        pluginNodeDefinition.pluginId(),
                        pluginNodeDefinition.nodeId()
                );
                JsonSchemaValidator.validate(config, nodeConfigSchema);
                log.debug("Node {} validated successfully", nodeSchema.getKey());
            } else {
                log.warn("Unable to validate node of type {} - not a plugin definition", type);
            }
        } catch (Exception e) {
            log.error("Error validating node schema: {}", e.getMessage(), e);
            throw new WorkflowNodeDefinitionException("Error validating node schema: " + e.getMessage(), e);
        }
    }

    public void removeNode(List<Map<String, Object>> nodes, String keyToRemove) {
        nodes.removeIf(node -> keyToRemove.equals(node.get("key")));
    }
}
