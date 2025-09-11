package org.phong.zenflow.workflow.subdomain.node_definition.definitions.config;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.Serializable;
import java.util.Map;

public record WorkflowConfigOutput(Map<String, Object> output) implements Serializable {
    @JsonCreator
    public WorkflowConfigOutput {
    }

    public WorkflowConfigOutput() {
        this(Map.of());
    }
}