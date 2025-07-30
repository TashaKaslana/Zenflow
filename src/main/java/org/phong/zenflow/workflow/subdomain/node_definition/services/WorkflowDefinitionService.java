package org.phong.zenflow.workflow.subdomain.node_definition.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.workflow.subdomain.context.WorkflowContextService;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowMetadata;
import org.phong.zenflow.workflow.subdomain.node_definition.exception.WorkflowDefinitionValidationException;
import org.phong.zenflow.workflow.subdomain.node_definition.exception.WorkflowNodeDefinitionException;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationResult;
import org.phong.zenflow.workflow.subdomain.schema_validator.service.WorkflowValidationService;
import org.phong.zenflow.workflow.utils.NodeKeyGenerator;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service responsible for managing workflow definitions.<br>
 * Provides functionality for creating, updating, validating, and manipulating workflow definitions.<br>
 * This service ensures that workflow definitions maintain integrity during operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowDefinitionService {

    private final WorkflowValidationService workflowValidationService;
    private final WorkflowContextService workflowContextService;

    /**
     * Updates or inserts nodes in the temporary definition based on the existing definition.
     * Preserves existing nodes and adds new ones, generating keys for nodes without them.
     *
     * @param existingDef The existing workflow definition containing current nodes
     * @param tempDef     The temporary workflow definition being built
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
                        key, node.getType(), node.getPluginNode(), node.getNext(),
                        node.getConfig(), node.getMetadata(), node.getPolicy()
                );
            }

            keyToNode.put(key, node);
        }

        existingDef.nodes().clear();
        existingDef.nodes().addAll(keyToNode.values());
    }

    /**
     * Updates or inserts metadata entries in the provided metadata map.
     * Skips null values and logs metadata operations for debugging.
     *
     * @param metadata The metadata map to update
     * @param updates  The new metadata entries to add or update
     */
    private static void upsertMetadata(WorkflowMetadata metadata, WorkflowMetadata updates) {
        if (updates == null) {
            log.debug("No metadata provided for upsert");
            return;
        }

        log.debug("Upserting metadata: {}", updates);
        if (metadata != null) {
            metadata.aliases().putAll(updates.aliases() != null ? updates.aliases() : Map.of());
            metadata.nodeDependencies().putAll(updates.nodeDependencies() != null ? updates.nodeDependencies() : Map.of());
            metadata.nodeConsumers().putAll(updates.nodeConsumers() != null ? updates.nodeConsumers() : Map.of());
        }
    }

    /**
     * Creates or updates a workflow definition by merging new definition with an existing one.
     *
     * @param newDef      The new workflow definition to be applied
     * @param existingDef The existing workflow definition to be updated
     * @return The updated workflow definition
     * @throws WorkflowNodeDefinitionException       If the new definition is null or any node has invalid properties
     * @throws WorkflowDefinitionValidationException If the resulting workflow fails in validation
     */
    public WorkflowDefinition upsert(WorkflowDefinition newDef, WorkflowDefinition existingDef) {
        if (newDef == null) {
            throw new WorkflowNodeDefinitionException("Workflow definition cannot be null");

        }

        upsertNodes(existingDef, newDef);
        upsertMetadata(existingDef.metadata(), newDef.metadata());

        return constructStaticMetadataAndValidate(existingDef);
    }

    private WorkflowDefinition constructStaticMetadataAndValidate(WorkflowDefinition tempDef) {
        WorkflowDefinition workflowDefinition = updateStaticContextMetadata(tempDef);

        ValidationResult validationResult = workflowValidationService.validateDefinition(workflowDefinition);
        if (!validationResult.isValid()) {
            log.debug("Workflow definition validation failed: {}", validationResult.getErrors());
            throw new WorkflowDefinitionValidationException("Workflow definition validation failed!", validationResult);
        }

        return workflowDefinition;
    }

    private WorkflowDefinition updateStaticContextMetadata(WorkflowDefinition definition) {
        if (definition == null) {
            definition = new WorkflowDefinition();
        }

        WorkflowMetadata metadata = definition.metadata();
        if (metadata == null) {
            metadata = new WorkflowMetadata();
        }

        WorkflowMetadata newMetadata = workflowContextService.buildStaticContext(definition.nodes(), metadata);

        return new WorkflowDefinition(definition.nodes(), newMetadata);
    }

    /**
     * Removes a node from the provided list of nodes based on its key.
     *
     * @param workflowDefinition The workflow definition containing the nodes
     * @param keyToRemove        The key of the node to remove
     * @return The updated workflow definition with the specified node removed
     * @throws WorkflowNodeDefinitionException       If the node with the specified key does not exist
     * @throws WorkflowDefinitionValidationException If the resulting workflow fails in validation
     */
    public WorkflowDefinition removeNode(WorkflowDefinition workflowDefinition, String keyToRemove) {
        workflowDefinition.nodes()
                .removeIf(node -> keyToRemove.equals(node.getKey()));
        return constructStaticMetadataAndValidate(workflowDefinition);
    }

    public WorkflowDefinition clearWorkflowDefinition(WorkflowDefinition workflowDefinition) {
        if (workflowDefinition == null) {
            return new WorkflowDefinition();
        }

        workflowDefinition.nodes().clear();
        workflowDefinition.metadata().aliases().clear();
        workflowDefinition.metadata().nodeDependencies().clear();
        workflowDefinition.metadata().nodeConsumers().clear();

        return workflowDefinition;
    }
}
