package org.phong.zenflow.workflow.subdomain.node_definition.definitions.config;

import java.io.Serializable;
import java.util.Map;

public record WorkflowConfigInput(Map<String, Object> input) implements Serializable {
    public WorkflowConfigInput() {
        this(Map.of());
    }
}
