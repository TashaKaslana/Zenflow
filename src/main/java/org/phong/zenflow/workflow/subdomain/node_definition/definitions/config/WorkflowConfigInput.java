package org.phong.zenflow.workflow.subdomain.node_definition.definitions.config;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.Serializable;
import java.util.Map;

public record WorkflowConfigInput(Map<String, Object> input) implements Serializable {
    @JsonCreator
    public WorkflowConfigInput {
    }

    public WorkflowConfigInput() {
        this(Map.of());
    }
}
