package org.phong.zenflow.workflow.subdomain.node_definition.definitions.config;

import java.io.Serializable;
import java.util.Map;

public record WorkflowConfigOutput(Map<String, Object> output) implements Serializable {
    public WorkflowConfigOutput() {
        this(Map.of());
    }
}
