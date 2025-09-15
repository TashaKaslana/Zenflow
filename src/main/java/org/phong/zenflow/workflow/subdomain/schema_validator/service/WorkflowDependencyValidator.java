package org.phong.zenflow.workflow.subdomain.schema_validator.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.workflow.subdomain.node_definition.WorkflowConstraints;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowMetadata;
import org.phong.zenflow.workflow.subdomain.node_definition.enums.NodeType;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationError;
import org.phong.zenflow.workflow.subdomain.schema_validator.enums.ValidationErrorCode;
import org.phong.zenflow.workflow.subdomain.evaluator.services.TemplateService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class WorkflowDependencyValidator {
    private final TemplateService templateService;

    public List<ValidationError> validateNodeDependencyLoops(WorkflowDefinition workflow) {

        // 1. Build execution order from 'next' relationships
        TopologicalOrderResult orderResult = buildTopologicalOrder(workflow);
        List<ValidationError> errors = new ArrayList<>(orderResult.errors());

        // If we have cycles, we can't continue with dependency validation
        if (orderResult.executionOrder() == null) {
            return errors;
        }

        // Validate that aliases only reference existing nodes
        errors.addAll(validateAliasDefinitions(workflow));

        // 2. Validate each node's dependencies against execution order
        List<String> executionOrder = orderResult.executionOrder();
        for (int i = 0; i < executionOrder.size(); i++) {
            String currentNode = executionOrder.get(i);
            Set<String> availableNodes = new HashSet<>(executionOrder.subList(0, i));

            errors.addAll(validateNodeDependencies(currentNode, availableNodes, workflow));
        }

        return errors;
    }

    private List<ValidationError> validateNodeDependencies(String nodeKey, Set<String> availableNodes,
                                                           WorkflowDefinition workflow) {
        List<ValidationError> errors = new ArrayList<>();
        WorkflowMetadata metadata = workflow.metadata();

        Set<String> dependencies = metadata.nodeDependencies().get(nodeKey);
        if (dependencies == null || dependencies.isEmpty()) {
            return errors;
        }

        Map<String, String> aliases = metadata.aliases();

        // Create a node map for quick lookup
        Map<String, BaseWorkflowNode> nodeMap = workflow.nodes().asMap();

        // Validate each dependency
        for (String dependency : dependencies) {
            if (WorkflowConstraints.isReservedKey(dependency)) {
                continue;
            }
            String sourceNode = templateService.getReferencedNode(dependency, aliases);

            if (sourceNode == null) {
                continue;
            }

            if (!availableNodes.contains(sourceNode)) {
                // Check if this is a loop node and the dependency is a self-reference
                boolean isSelfReference = sourceNode.equals(nodeKey);
                boolean isLoopNode = isLoopNode(nodeKey, nodeMap);

                // Allow self-references for loop nodes (they need to reference their own output)
                if (isSelfReference && isLoopNode) {
                    log.debug("Allowing self-reference '{}' for loop node '{}'", dependency, nodeKey);
                    continue;
                }

                errors.add(ValidationError.builder()
                        .nodeKey(nodeKey)
                        .errorType("definition")
                        .errorCode(ValidationErrorCode.INVALID_CONNECTION)
                        .path("nodes." + nodeKey + ".dependencies")
                        .message(String.format("Node '%s' references future node or not exists node '%s' in dependency: '%s'.",
                                nodeKey, sourceNode, dependency))
                        .value(dependency)
                        .template(dependency)
                        .expectedType("previous_node_reference")
                        .schemaPath("$.nodes[?(@.key=='" + nodeKey + "')].config")
                        .build());
            }
        }

        return errors;
    }

    private TopologicalOrderResult buildTopologicalOrder(WorkflowDefinition workflow) {
        List<ValidationError> errors = new ArrayList<>();
        Map<String, BaseWorkflowNode> nodeMap = workflow.nodes().asMap();
        Map<String, String> aliases = workflow.metadata() != null ? workflow.metadata().aliases() : Collections.emptyMap();

        if (nodeMap == null || nodeMap.isEmpty()) {
            return new TopologicalOrderResult(new ArrayList<>(), errors);
        }

        // Build adjacency list and in-degree map
        Map<String, List<String>> adjacencyList = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();

        // Step 1: Initialize nodes
        for (Map.Entry<String, BaseWorkflowNode> entry : nodeMap.entrySet()) {
            String nodeKey = entry.getKey();
            List<String> nextKeys = entry.getValue().getNext();
            if (nextKeys == null) {
                nextKeys = Collections.emptyList();
            }

            adjacencyList.put(nodeKey, new ArrayList<>(nextKeys));
            inDegree.put(nodeKey, 0);
        }

        // Step 2: Validate next references and compute in-degrees
        for (Map.Entry<String, List<String>> entry : adjacencyList.entrySet()) {
            String nodeKey = entry.getKey();
            List<String> nextKeys = entry.getValue();

            for (String nextNode : nextKeys) {
                if (inDegree.containsKey(nextNode)) {
                    inDegree.put(nextNode, inDegree.get(nextNode) + 1);
                } else {
                    errors.add(ValidationError.builder()
                            .nodeKey(nodeKey)
                            .errorType("definition")
                            .errorCode(ValidationErrorCode.MISSING_NODE_REFERENCE)
                            .path("nodes." + nodeKey + ".next")
                            .message(String.format("Node '%s' references non-existent next node: '%s'.", nodeKey, nextNode))
                            .value(nextNode)
                            .expectedType("existing_node_key")
                            .schemaPath("$.nodes[?(@.key=='" + nodeKey + "')].next")
                            .build());
                }
            }
        }

        // Step 2.5: Add edges based on explicit nodeDependencies
        WorkflowMetadata metadata = workflow.metadata();
        if (metadata != null && metadata.nodeDependencies() != null) {
            for (Map.Entry<String, Set<String>> entry : metadata.nodeDependencies().entrySet()) {
                String nodeKey = entry.getKey();
                Set<String> dependencies = entry.getValue();

                for (String dependency : dependencies) {
                    String referencedNode = templateService.getReferencedNode(dependency, aliases);
                    if (referencedNode != null && !referencedNode.equals(nodeKey) && inDegree.containsKey(referencedNode)) {
                        adjacencyList.get(referencedNode).add(nodeKey);
                        inDegree.put(nodeKey, inDegree.get(nodeKey) + 1);
                    }
                }
            }
        }

        // Step 3: Handle loop nodes - temporarily break their back-edges for topological sorting
        Map<String, List<String>> loopBackEdges = new HashMap<>();

        for (Map.Entry<String, BaseWorkflowNode> entry : nodeMap.entrySet()) {
            String nodeKey = entry.getKey();
            BaseWorkflowNode node = entry.getValue();
            if (isLoopNode(node)) {
                List<String> nextNodes = adjacencyList.get(nodeKey);
                List<String> backEdges = new ArrayList<>();

                for (String nextNode : new ArrayList<>(nextNodes)) {
                    if (adjacencyList.getOrDefault(nextNode, List.of()).contains(nodeKey)) {
                        backEdges.add(nextNode);
                        adjacencyList.get(nextNode).remove(nodeKey);
                        inDegree.put(nodeKey, inDegree.get(nodeKey) - 1);
                    }
                }

                if (!backEdges.isEmpty()) {
                    loopBackEdges.put(nodeKey, backEdges);
                }
            }
        }

        // Step 4: Topological sort using Kahn's algorithm
        Queue<String> queue = new LinkedList<>();
        List<String> executionOrder = new ArrayList<>();

        inDegree.entrySet().stream()
                .filter(entry -> entry.getValue() == 0)
                .forEach(entry -> queue.offer(entry.getKey()));

        while (!queue.isEmpty()) {
            String current = queue.poll();
            executionOrder.add(current);

            for (String neighbor : adjacencyList.getOrDefault(current, List.of())) {
                if (inDegree.containsKey(neighbor)) {
                    int updated = inDegree.get(neighbor) - 1;
                    inDegree.put(neighbor, updated);
                    if (updated == 0) {
                        queue.offer(neighbor);
                    }
                }
            }
        }

        // Step 5: Check for cycle (excluding loop nodes with intentional back edges)
        if (executionOrder.size() != nodeMap.size()) {
            Set<String> visited = new HashSet<>(executionOrder);
            Set<String> cycleNodes = nodeMap.keySet().stream()
                    .filter(key -> !visited.contains(key))
                    .collect(Collectors.toSet());

            // Check if all cycle nodes are part of valid loop structures
            Set<String> invalidCycleNodes = cycleNodes.stream()
                    .filter(nodeKey -> !isValidLoopCycle(nodeKey, nodeMap, loopBackEdges))
                    .collect(Collectors.toSet());

            if (!invalidCycleNodes.isEmpty()) {
                String firstInCycle = invalidCycleNodes.stream().findFirst().orElse("unknown");

                errors.add(ValidationError.builder()
                        .nodeKey(firstInCycle)
                        .errorType("definition")
                        .errorCode(ValidationErrorCode.CIRCULAR_DEPENDENCY)
                        .path("workflow.nodes")
                        .message("Workflow contains cycles. Cycle detected involving node: '" + firstInCycle + "'.")
                        .value(new ArrayList<>(invalidCycleNodes))
                        .expectedType("acyclic_graph")
                        .schemaPath("$.nodes")
                        .build());

                return new TopologicalOrderResult(null, errors);
            }
        }

        // Step 6: Restore loop back edges for execution order
        for (Map.Entry<String, List<String>> entry : loopBackEdges.entrySet()) {
            String loopNode = entry.getKey();
            for (String backEdgeNode : entry.getValue()) {
                adjacencyList.get(backEdgeNode).add(loopNode);
            }
        }

        return new TopologicalOrderResult(executionOrder, errors);
    }

    private boolean isLoopNode(BaseWorkflowNode node) {
        // Check if the node type is one of the predefined loop types
        return NodeType.getLoopStatefulTypes().contains(node.getType());
    }

    /**
     * Check if a node is a loop node based on its key and metadata
     * This overloaded method is used when we only have the nodeKey
     */
    private boolean isLoopNode(String nodeKey, Map<String, BaseWorkflowNode> nodeMap) {
        BaseWorkflowNode node = nodeMap.get(nodeKey);
        return node != null && isLoopNode(node);
    }

    private boolean isValidLoopCycle(String nodeKey, Map<String, BaseWorkflowNode> nodeMap, Map<String, List<String>> loopBackEdges) {
        BaseWorkflowNode node = nodeMap.get(nodeKey);
        if (node == null) return false;

        // If this node is part of a loop structure with registered back edges, it's valid
        for (Map.Entry<String, List<String>> entry : loopBackEdges.entrySet()) {
            String loopNode = entry.getKey();
            List<String> backEdgeNodes = entry.getValue();

            if (nodeKey.equals(loopNode) || backEdgeNodes.contains(nodeKey)) {
                return true;
            }
        }

        return false;
    }

    private List<ValidationError> validateAliasDefinitions(WorkflowDefinition workflow) {
        List<ValidationError> errors = new ArrayList<>();
        Map<String, String> aliases = workflow.metadata().aliases();

        if (aliases == null || aliases.isEmpty()) {
            return errors;
        }

        Set<String> allNodeKeys = workflow.nodes().keys();

        for (Map.Entry<String, String> aliasEntry : aliases.entrySet()) {
            String aliasName = aliasEntry.getKey();
            String aliasValue = aliasEntry.getValue(); // e.g., "{{node1.output.email}}"

            // Extract template references from aliases value
            Set<String> aliasRefs = templateService.extractRefs(aliasValue);

            for (String ref : aliasRefs) {
                // For aliases validation, we don't pass aliases to avoid circular resolution
                String sourceNode = extractNodeKeyFromRef(ref);

                if (sourceNode != null && !allNodeKeys.contains(sourceNode)) {
                    errors.add(ValidationError.builder()
                            .nodeKey("aliases") // Alias is global, but we can specify the group
                            .errorType("definition")
                            .errorCode(ValidationErrorCode.MISSING_NODE_REFERENCE)
                            .path("metadata.aliases." + aliasName)
                            .message(String.format("Alias '%s' references a non-existent node '%s'.",
                                    aliasName, sourceNode))
                            .value(aliasValue)
                            .template(ref)
                            .expectedType("existing_node_key")
                            .schemaPath("$.metadata.aliases['" + aliasName + "']")
                            .build());
                }
            }
        }

        return errors;
    }

    /**
     * Simple node key extraction for aliases validation
     */
    private String extractNodeKeyFromRef(String ref) {
        if (ref == null || ref.trim().isEmpty()) {
            return null;
        }

        if (ref.contains(".output.")) {
            return ref.split("\\.output\\.")[0];
        }

        String[] parts = ref.split("\\.");
        if (parts.length >= 2) {
            return parts[0];
        }

        return null;
    }

    /**
     * Result class for topological ordering
     */
    private record TopologicalOrderResult(List<String> executionOrder, List<ValidationError> errors) {
    }
}
