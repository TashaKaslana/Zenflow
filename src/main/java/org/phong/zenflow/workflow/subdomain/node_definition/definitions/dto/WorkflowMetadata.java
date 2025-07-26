package org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto;

import java.util.Map;
import java.util.Set;

public record WorkflowMetadata(Map<String, String> alias, Map<String, Set<String>> nodeDependency, Map<String, OutputUsage> nodeConsumer) {
    public WorkflowMetadata() {
        this(Map.of(), Map.of(), Map.of());
    }
}
