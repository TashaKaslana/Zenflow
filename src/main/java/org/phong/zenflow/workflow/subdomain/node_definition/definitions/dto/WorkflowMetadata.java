package org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto;

import java.util.Map;

public record WorkflowMetadata(Map<String, String> alias, Map<String, Object> nodeDependency, Map<String, Object> nodeConsumer) {
    public WorkflowMetadata() {
        this(Map.of(), Map.of(), Map.of());
    }

    public WorkflowMetadata(Map<String, String> alias) {
        this(alias, Map.of(), Map.of());
    }

    public WorkflowMetadata(Map<String, String> alias, Map<String, Object> nodeDependency) {
        this(alias, nodeDependency, Map.of());
    }
}
