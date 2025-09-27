package org.phong.zenflow.workflow.subdomain.node_definition.definitions.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

public record WorkflowConfigProfile(List<String> profileKeys) implements Serializable {

    public WorkflowConfigProfile {
        profileKeys = profileKeys == null ? List.of() : List.copyOf(profileKeys);
    }

    @JsonCreator
    public static WorkflowConfigProfile fromJson(
            @JsonProperty("profileKeys") List<String> profileKeys,
            @JsonProperty("profileNames") List<String> legacyProfileNames
    ) {
        List<String> keys = profileKeys != null ? profileKeys : legacyProfileNames;
        return new WorkflowConfigProfile(keys == null ? List.of() : keys);
    }

    public WorkflowConfigProfile() {
        this(List.of());
    }
}
