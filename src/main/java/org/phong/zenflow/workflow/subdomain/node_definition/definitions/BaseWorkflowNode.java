package org.phong.zenflow.workflow.subdomain.node_definition.definitions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;

import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.phong.zenflow.workflow.subdomain.node_definition.enums.NodeType;

import java.util.List;
import java.util.Map;

@Setter
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

    @NotNull
    private final WorkflowConfig config;

    private final Map<String, Object> metadata;

    private final Map<String, Object> policy;

    @NotNull
    private final PluginNodeIdentifier pluginNode;
}