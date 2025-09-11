package org.phong.zenflow.workflow.subdomain.node_definition.definitions.config;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.Serializable;
import java.util.Map;

public record WorkflowConfigProfile(Map<String, Object> profileKeys) implements Serializable {
    @JsonCreator
    public WorkflowConfigProfile {
    }

    public WorkflowConfigProfile() {
        this(Map.of());
    }
}
