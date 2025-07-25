package org.phong.zenflow.workflow.subdomain.node_definition.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.exception.WorkflowDefinitionValidationException;
import org.phong.zenflow.workflow.subdomain.node_definition.exception.WorkflowNodeDefinitionException;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationResult;
import org.phong.zenflow.workflow.subdomain.schema_validator.service.WorkflowValidationService;
import org.phong.zenflow.workflow.utils.NodeKeyGenerator;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service responsible for managing workflow definitions.
 * Provides functionality for creating, updating, validating, and manipulating workflow definitions.
 * This service ensures that workflow definitions maintain integrity during operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowDefinitionService {

    private final WorkflowValidationService workflowValidationService;

    /**
     * Creates or updates a workflow definition by merging new definition with an existing one.
     *
     * @param newDef The new workflow definition to be applied
     * @param existingDef The existing workflow definition to be updated
     * @return The updated workflow definition
     * @throws WorkflowNodeDefinitionException If the new definition is null or any node has invalid properties
     * @throws WorkflowDefinitionValidationException If the resulting workflow fails in validation
     */
    public WorkflowDefinition upsert(WorkflowDefinition newDef, WorkflowDefinition existingDef) {
        if (newDef == null) {
            throw new WorkflowNodeDefinitionException("Workflow definition cannot be null");

        }
        WorkflowDefinition tempDef = new WorkflowDefinition(newDef);

        upsertNodes(existingDef, tempDef);
        upsertMetadata(tempDef.metadata(), newDef.metadata());

        ValidationResult validationResult = workflowValidationService.validateDefinition(tempDef);
        if (!validationResult.isValid()) {
            log.error("Workflow definition validation failed: {}", validationResult.getErrors());
            throw new WorkflowDefinitionValidationException("Workflow definition validation failed!", validationResult);
        }

        return tempDef;
    }

    /**
     * Updates or inserts nodes in the temporary definition based on the existing definition.
     * Preserves existing nodes and adds new ones, generating keys for nodes without them.
     *
     * @param existingDef The existing workflow definition containing current nodes
     * @param tempDef The temporary workflow definition being built
     * @throws WorkflowNodeDefinitionException If any node has an empty type
     */
    private static void upsertNodes(WorkflowDefinition existingDef, WorkflowDefinition tempDef) {
        Map<String, BaseWorkflowNode> keyToNode = existingDef.nodes().stream()
                .collect(Collectors.toMap(
                        BaseWorkflowNode::getKey,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        for (BaseWorkflowNode node : tempDef.nodes()) {
            String type = node.getType().name();
            if (type.isBlank()) {
                throw new WorkflowNodeDefinitionException("Each node must have a 'type'");
            }

            String key = node.getKey();
            if (key == null || key.isBlank()) {
                key = NodeKeyGenerator.generateKey(type);
                node = new BaseWorkflowNode(
                        key, node.getType(), node.getNext(),
                        node.getConfig(), node.getMetadata(), node.getPolicy()
                );
            }

            keyToNode.put(key, node);
        }

        tempDef.nodes().clear();
        tempDef.nodes().addAll(keyToNode.values());
    }

    /**
     * Updates or inserts metadata entries in the provided metadata map.
     * Skips null values and logs metadata operations for debugging.
     *
     * @param metadata The metadata map to update
     * @param updates The new metadata entries to add or update
     */
    private static void upsertMetadata(Map<String, Object> metadata, Map<String, Object> updates) {
        if (metadata == null || updates.isEmpty()) {
            log.debug("No metadata provided for upsert");
            return;
        }

        log.debug("Upserting metadata: {}", updates);
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value == null) {
                log.warn("Skipping null value for metadata key: {}", key);
                continue;
            }

            if (metadata.containsKey(key)) {
                log.debug("Updating existing metadata key: {}", key);
            } else {
                log.debug("Adding new metadata key: {}", key);
            }
            metadata.put(key, value);
        }
    }

    /**
     * Removes a node from the provided list of nodes based on its key.
     *
     * @param nodes The list of workflow nodes to modify
     * @param keyToRemove The key of the node to remove
     */
    public void removeNode(List<BaseWorkflowNode> nodes, String keyToRemove) {
        nodes.removeIf(node -> keyToRemove.equals(node.getKey()));
    }
}
