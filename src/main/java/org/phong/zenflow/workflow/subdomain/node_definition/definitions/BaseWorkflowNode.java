package org.phong.zenflow.workflow.subdomain.node_definition.definitions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.trigger.TriggerNodeDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.enums.NodeType;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = PluginDefinition.class, name = "PLUGIN"),
        @JsonSubTypes.Type(value = TriggerNodeDefinition.class, name = "TRIGGER")
})
public class BaseWorkflowNode {
    @NotNull
    private final String key;

    @NotNull
    private final NodeType type;

    @NotNull
    private final List<String> next;

    private final WorkflowConfig config;

    private final Map<String, Object> metadata;

    private final Map<String, Object> policy;
}