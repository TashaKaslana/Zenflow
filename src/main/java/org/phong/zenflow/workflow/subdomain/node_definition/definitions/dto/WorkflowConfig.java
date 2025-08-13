package org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto;

import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.Map;

public record WorkflowConfig(Map<String, Object> input, @Nullable Map<String, Object> output, @Nullable Map<String, Object> entrypoint) implements Serializable {
    public WorkflowConfig() {
        this(Map.of(), Map.of(), null);
    }

    public WorkflowConfig(Map<String, Object> input, Map<String, Object> output) {
        this(input, output, null);
    }

    public WorkflowConfig(Map<String, Object> input) {
        this(input, null, null);
    }
}
