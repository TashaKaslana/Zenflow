package org.phong.zenflow.workflow.subdomain.node_definition.definitions.config;

import java.io.Serializable;
import java.util.Map;

public record WorkflowConfigProfile(Map<String, Object> profileKeys) implements Serializable {
    public WorkflowConfigProfile() {
        this(Map.of());
    }
}
