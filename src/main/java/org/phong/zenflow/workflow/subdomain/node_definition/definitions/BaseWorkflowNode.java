package org.phong.zenflow.workflow.subdomain.node_definition.definitions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.phong.zenflow.workflow.subdomain.node_definition.enums.NodeType;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseWorkflowNode {
    @NotNull
    private final String key;

    @NotNull
    private final NodeType type;

    @NotNull
    private final List<String> next;

    private final Map<String, Object> config;

    private final Map<String, Object> metadata;

    private final Map<String, Object> policy;
}