package org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto;

import java.util.Map;
import java.util.Set;

public record WorkflowMetadata(Map<String, String> aliases, Map<String, Set<String>> nodeDependencies, Map<String, OutputUsage> nodeConsumers) {
    public WorkflowMetadata() {
        this(Map.of(), Map.of(), Map.of());
    }
}
