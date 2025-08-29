package org.phong.zenflow.workflow.subdomain.node_definition.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.projections.PluginNodeId;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.repository.PluginNodeRepository;
import org.phong.zenflow.workflow.subdomain.context.WorkflowContextService;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowMetadata;
import org.phong.zenflow.workflow.subdomain.node_definition.exception.WorkflowDefinitionValidationException;
import org.phong.zenflow.workflow.subdomain.node_definition.exception.WorkflowNodeDefinitionException;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationError;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationResult;
import org.phong.zenflow.workflow.subdomain.schema_validator.enums.ValidationErrorCode;
import org.phong.zenflow.workflow.subdomain.schema_validator.service.WorkflowValidationService;
import org.phong.zenflow.workflow.utils.NodeKeyGenerator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
    private final PluginNodeRepository pluginNodeRepository;

    /**
     * Updates or inserts nodes in the temporary definition based on the existing definition.
     * Preserves existing nodes and adds new ones, generating keys for nodes without them.
     *
     * @param existingDef The existing workflow definition containing current nodes
     * @param tempDef     The temporary workflow definition being built
     * @throws WorkflowNodeDefinitionException If any node has an empty type
     */
    private void upsertNodes(WorkflowDefinition existingDef, WorkflowDefinition tempDef) {
        Map<String, BaseWorkflowNode> keyToNode = existingDef.nodes().asMap();

        tempDef.nodes().forEach((ignored, node) -> {
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
        });

        existingDef.nodes().clear();
        keyToNode.forEach((k, node) -> existingDef.nodes().put(node));
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

        WorkflowDefinition tempDef = new WorkflowDefinition(existingDef);

        upsertNodes(tempDef, newDef);
        upsertMetadata(tempDef.metadata(), newDef.metadata());

        return constructStaticGenerationAndValidate(tempDef);
    }

    private WorkflowDefinition constructStaticGenerationAndValidate(WorkflowDefinition tempDef) {
        List<ValidationError> validationErrors = new ArrayList<>();

        resolvePluginId(tempDef, validationErrors);
        updateStaticContextMetadata(tempDef);

        ValidationResult validationResult = workflowValidationService.validateDefinition(tempDef);
        validationResult.addAllErrors(validationErrors);

        if (!validationResult.isValid()) {
            log.debug("Workflow definition validation failed: {}", validationResult.getErrors());
            throw new WorkflowDefinitionValidationException("Workflow definition validation failed!", validationResult);
        }

        return tempDef;
    }

    private void updateStaticContextMetadata(WorkflowDefinition definition) {
        if (definition == null) {
            definition = new WorkflowDefinition();
        }

        WorkflowMetadata newMetadata = workflowContextService.buildStaticContext(definition);

        new WorkflowDefinition(definition.nodes(), newMetadata);
    }

    private void resolvePluginId(WorkflowDefinition workflowDefinition, List<ValidationError> validationErrors) {
        Set<String> compositeKeys = workflowDefinition.nodes().getPluginNodeCompositeKeys();
        if (compositeKeys.isEmpty()) {
            return;
        }

        Set<PluginNodeId> pluginNodeIds = pluginNodeRepository.findIdsByCompositeKeys(compositeKeys);

        Map<String, UUID> compositeKeyToIdMap = new HashMap<>();
        pluginNodeIds.forEach(pluginNodeId ->
            compositeKeyToIdMap.put(pluginNodeId.getCompositeKey(), pluginNodeId.getId()));

        // Update workflow nodes with the resolved UUIDs
        for (Map.Entry<String, BaseWorkflowNode> nodeEntry : workflowDefinition.nodes().asMap().entrySet()) {
            BaseWorkflowNode workflowNode = nodeEntry.getValue();
            String compositeKey = workflowNode.getPluginNode().toCacheKey();

            if (compositeKeyToIdMap.containsKey(compositeKey)) {
                workflowNode.getPluginNode().setNodeId(compositeKeyToIdMap.get(compositeKey));
            } else {
                validationErrors.add(
                        ValidationError.builder()
                                .nodeKey(nodeEntry.getKey())
                                .errorCode(ValidationErrorCode.VALIDATION_ERROR)
                                .errorType("definition")
                                .path("nodes.pluginNode.nodeId")
                                .message("Plugin Node doesn't exist with composite key: " + compositeKey)
                                .build()
                );
            }
        }

    }

    /**
     * Removes multiple nodes from the provided workflow definition based on their keys.
     *
     * @param workflowDefinition The workflow definition containing the nodes
     * @param keysToRemove       The keys of the nodes to remove
     * @return The updated workflow definition with the specified nodes removed
     */
    public WorkflowDefinition removeNodes(WorkflowDefinition workflowDefinition, List<String> keysToRemove) {
        if (workflowDefinition == null) {
            return new WorkflowDefinition();
        }
        if (keysToRemove == null || keysToRemove.isEmpty()) {
            return workflowDefinition;
        }

        keysToRemove.forEach(workflowDefinition.nodes()::remove);
        return constructStaticGenerationAndValidate(workflowDefinition);
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
