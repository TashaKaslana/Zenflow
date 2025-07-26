package org.phong.zenflow.workflow.subdomain.node_definition.definitions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowMetadata;

import java.io.Serializable;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowDefinition(List<BaseWorkflowNode> nodes, WorkflowMetadata metadata) implements Serializable {
    public WorkflowDefinition() {
        this(List.of(), new WorkflowMetadata());
    }

    public WorkflowDefinition(WorkflowDefinition newDef) {
        this(newDef.nodes, newDef.metadata);
    }
}
