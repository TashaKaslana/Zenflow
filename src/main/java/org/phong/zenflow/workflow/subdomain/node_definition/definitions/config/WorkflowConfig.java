package org.phong.zenflow.workflow.subdomain.node_definition.definitions.config;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public final class WorkflowConfig implements Serializable {
    @JsonUnwrapped
    private final WorkflowConfigInput input;

    @Nullable
    @JsonUnwrapped
    private final WorkflowConfigProfile profile;

    @Nullable
    @JsonUnwrapped
    private final WorkflowConfigOutput output;


    public WorkflowConfig() {
        this(new WorkflowConfigInput(), new WorkflowConfigProfile(), new WorkflowConfigOutput());
    }

    public WorkflowConfig(Map<String, Object> input,  List<String> profileNames) {
        this(new WorkflowConfigInput(input), new WorkflowConfigProfile(profileNames), null);
    }

    public WorkflowConfig(Map<String, Object> input) {
        this(new WorkflowConfigInput(input), null, null);
    }

    public WorkflowConfig(Map<String, Object> input, List<String> profileNames, Map<String, Object> output) {
        this(new WorkflowConfigInput(input), new WorkflowConfigProfile(profileNames), new WorkflowConfigOutput(output));
    }

    public Map<String, Object> input() {
        return input.input();
    }

    public <T> T getInputValue(String key, Class<T> clazz) {
        Object value = input.input().get(key);
        if (value == null) {
            return null;
        }
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        } else {
            throw new ClassCastException("Cannot cast input value to " + clazz.getName());
        }
    }

    public Map<String, Object> output() {
        return output != null ? output.output() : Map.of();
    }

    public List<String> profile() {
        return profile != null ? profile.profileNames() : List.of();
    }
}
