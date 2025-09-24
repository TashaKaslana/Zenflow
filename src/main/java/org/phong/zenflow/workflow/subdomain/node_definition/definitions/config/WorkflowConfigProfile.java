package org.phong.zenflow.workflow.subdomain.node_definition.definitions.config;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.Serializable;
import java.util.List;

public record WorkflowConfigProfile(List<String> profileNames) implements Serializable {
    @JsonCreator
    public WorkflowConfigProfile {
    }

    public WorkflowConfigProfile() {
        this(List.of());
    }
}
