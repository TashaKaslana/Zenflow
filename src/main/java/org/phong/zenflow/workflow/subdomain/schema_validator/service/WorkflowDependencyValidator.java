package org.phong.zenflow.workflow.subdomain.schema_validator.service;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.execution.utils.TemplateEngine;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowMetadata;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationError;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@AllArgsConstructor
public class WorkflowDependencyValidator {

    public List<ValidationError> validateNodeDependencyLoops(WorkflowDefinition workflow) {

        // 1. Build execution order from 'next' relationships
        TopologicalOrderResult orderResult = buildTopologicalOrder(workflow.nodes());
        List<ValidationError> errors = new ArrayList<>(orderResult.errors());

        // If we have cycles, we can't continue with dependency validation
        if (orderResult.executionOrder() == null) {
            return errors;
        }

        // 2. Validate each node's dependencies against execution order
        List<String> executionOrder = orderResult.executionOrder();
        for (int i = 0; i < executionOrder.size(); i++) {
            String currentNode = executionOrder.get(i);
            Set<String> availableNodes = new HashSet<>(executionOrder.subList(0, i));

            errors.addAll(validateNodeDependencies(currentNode, availableNodes, workflow.metadata()));
        }

        return errors;
    }

    private List<ValidationError> validateNodeDependencies(String nodeKey, Set<String> availableNodes,
                                                           WorkflowMetadata metadata) {
        List<ValidationError> errors = new ArrayList<>();

        Set<String> dependencies = metadata.nodeDependencies().get(nodeKey);
        if (dependencies == null || dependencies.isEmpty()) {
            return errors;
        }

        Map<String, String> aliases = metadata.aliases();

        // Validate each dependency
        for (String dependency : dependencies) {
            String sourceNode = TemplateEngine.getReferencedNode(dependency, aliases);

            if (sourceNode != null && !availableNodes.contains(sourceNode)) {
                errors.add(ValidationError.builder()
                        .type("FUTURE_NODE_REFERENCE")
                        .path("nodes." + nodeKey + ".dependencies")
                        .message(String.format("Node '%s' references future node '%s' in dependency: %s",
                                nodeKey, sourceNode, dependency))
                        .value(dependency)
                        .template(dependency)
                        .expectedType("previous_node_reference")
                        .schemaPath("$.nodes[?(@.key=='" + nodeKey + "')].config")
                        .build());
            }
        }

        // Validate aliases used by this node don't reference future nodes
        errors.addAll(validateAliasesForNode(availableNodes, aliases));

        return errors;
    }

    private TopologicalOrderResult buildTopologicalOrder(List<BaseWorkflowNode> nodes) {
        List<ValidationError> errors = new ArrayList<>();

        if (nodes == null || nodes.isEmpty()) {
            return new TopologicalOrderResult(new ArrayList<>(), errors);
        }

        // Build an adjacency list from 'next' relationships
        Map<String, List<String>> adjacencyList = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();

        // Initialize all nodes
        for (BaseWorkflowNode node : nodes) {
            String nodeKey = node.getKey();
            adjacencyList.put(nodeKey, new ArrayList<>(node.getNext()));
            inDegree.put(nodeKey, 0);
        }

        // Calculate in-degrees and validate next references
        for (BaseWorkflowNode node : nodes) {
            for (String nextNode : node.getNext()) {
                if (inDegree.containsKey(nextNode)) {
                    inDegree.put(nextNode, inDegree.get(nextNode) + 1);
                } else {
                    errors.add(ValidationError.builder()
                            .type("INVALID_NEXT_REFERENCE")
                            .path("nodes." + node.getKey() + ".next")
                            .message(String.format("Node '%s' references non-existent next node: %s",
                                    node.getKey(), nextNode))
                            .value(nextNode)
                            .expectedType("existing_node_key")
                            .schemaPath("$.nodes[?(@.key=='" + node.getKey() + "')].next")
                            .build());
                }
            }
        }

        // If we have invalid references, we might still be able to continue
        // Kahn's algorithm for topological sort
        Queue<String> queue = new LinkedList<>();
        List<String> executionOrder = new ArrayList<>();

        // Find nodes with no incoming edges (start nodes)
        inDegree.entrySet().stream()
                .filter(entry -> entry.getValue() == 0)
                .forEach(entry -> queue.offer(entry.getKey()));

        while (!queue.isEmpty()) {
            String currentNode = queue.poll();
            executionOrder.add(currentNode);

            // Remove edges and update in-degrees
            for (String neighbor : adjacencyList.get(currentNode)) {
                if (inDegree.containsKey(neighbor)) {
                    int newInDegree = inDegree.get(neighbor) - 1;
                    inDegree.put(neighbor, newInDegree);

                    if (newInDegree == 0) {
                        queue.offer(neighbor);
                    }
                }
            }
        }

        // Check for cycles
        if (executionOrder.size() != nodes.size()) {
            errors.add(ValidationError.builder()
                    .type("CIRCULAR_DEPENDENCY")
                    .path("workflow.nodes")
                    .message("Workflow contains cycles - cannot determine execution order")
                    .value(nodes.stream().map(BaseWorkflowNode::getKey).toList())
                    .expectedType("acyclic_graph")
                    .schemaPath("$.nodes")
                    .build());

            // Return null execution order to indicate we can't continue
            return new TopologicalOrderResult(null, errors);
        }

        return new TopologicalOrderResult(executionOrder, errors);
    }

    private List<ValidationError> validateAliasesForNode(Set<String> availableNodes,
                                                         Map<String, String> aliases) {
        List<ValidationError> errors = new ArrayList<>();

        if (aliases == null || aliases.isEmpty()) {
            return errors;
        }

        for (Map.Entry<String, String> aliasEntry : aliases.entrySet()) {
            String aliasName = aliasEntry.getKey();
            String aliasValue = aliasEntry.getValue(); // e.g., "{{node1.output.email}}"

            // Extract template references from aliases value
            Set<String> aliasRefs = TemplateEngine.extractRefs(aliasValue);

            for (String ref : aliasRefs) {
                // For aliases validation, we don't pass aliases to avoid circular resolution
                String sourceNode = extractNodeKeyFromRef(ref);

                if (sourceNode != null && !availableNodes.contains(sourceNode)) {
                    errors.add(ValidationError.builder()
                            .type("FUTURE_NODE_REFERENCE_IN_ALIAS")
                            .path("metadata.aliases." + aliasName)
                            .message(String.format("Alias '%s' references future node '%s'. " +
                                            "Aliases can only reference previously executed nodes.",
                                    aliasName, sourceNode))
                            .value(aliasValue)
                            .template(ref)
                            .expectedType("previous_node_reference")
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
