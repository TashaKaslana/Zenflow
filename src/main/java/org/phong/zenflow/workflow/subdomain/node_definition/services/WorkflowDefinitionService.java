package org.phong.zenflow.workflow.subdomain.node_definition.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.phong.zenflow.plugin.subdomain.node.utils.JsonSchemaValidator;
import org.phong.zenflow.plugin.subdomain.node.utils.SchemaRegistry;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.trigger.TriggerNodeDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.enums.NodeType;
import org.phong.zenflow.workflow.subdomain.node_definition.exception.WorkflowNodeDefinitionException;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;
import org.phong.zenflow.workflow.utils.NodeKeyGenerator;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowDefinitionService {

    private final SchemaRegistry schemaRegistry;

    /**
     * Upsert (update if exists, insert if not). Auto-generates key if missing.
     * Validates each node as it's being upserted.
     */
    public void upsertNodes(List<BaseWorkflowNode> nodes, List<BaseWorkflowNode> updates) {
        Map<String, BaseWorkflowNode> keyToNode = nodes.stream()
                .collect(Collectors.toMap(
                        BaseWorkflowNode::getKey,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        for (BaseWorkflowNode update : updates) {
            String type = update.getType().name();
            if (type.isBlank()) {
                throw new WorkflowNodeDefinitionException("Each node must have a 'type'");
            }

            String key = update.getKey();
            if (key == null || key.isBlank()) {
                key = NodeKeyGenerator.generateKey(type);
                update = new BaseWorkflowNode(
                        key, update.getType(), update.getNext(),
                        update.getConfig(), update.getMetadata(), update.getPolicy()
                );
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
     * @param node The node to validate
     * @throws WorkflowNodeDefinitionException if validation fails
     */
    private void validateNode(BaseWorkflowNode node) {
        try {
            String type = node.getType().name();
            log.debug("Validating node type '{}'", type);

            if (type.equalsIgnoreCase(NodeType.TRIGGER.name()) && node instanceof TriggerNodeDefinition triggerNode) {
                String triggerType = triggerNode.getTriggerType().getType();

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

            log.debug("Validating node schema for key: {}, config: {}", node.getKey(), node.getConfig());
            JSONObject config = new JSONObject(node.getConfig());

            if (node instanceof PluginDefinition pluginDefinition) {
                PluginNodeDefinition pluginNodeDef = pluginDefinition.getPluginNode();
                if (pluginNodeDef == null) {
                    throw new WorkflowNodeDefinitionException("Plugin node definition is missing for node: " + node.getKey());
                }

                JSONObject schema = schemaRegistry.getPluginSchema(pluginNodeDef.pluginId(), pluginNodeDef.nodeId());
                JsonSchemaValidator.validate(schema, config);
                log.debug("Node schema validation successful for key: {}", node.getKey());
            } else {
                log.warn("Unable to validate node of type {} - not a plugin definition", type);
            }
        } catch (Exception e) {
            throw new WorkflowNodeDefinitionException("Node validation failed for key " + node.getKey(), e);
        }
    }

    public void removeNode(List<BaseWorkflowNode> nodes, String keyToRemove) {
        nodes.removeIf(node -> keyToRemove.equals(node.getKey()));
    }
}
