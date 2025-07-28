package org.phong.zenflow.workflow.subdomain.node_definition.definitions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowMetadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowDefinition(List<BaseWorkflowNode> nodes, WorkflowMetadata metadata) implements Serializable {
    public WorkflowDefinition() {
        this(new ArrayList<>(), new WorkflowMetadata());
    }

    public WorkflowDefinition(WorkflowDefinition newDef) {
        this(newDef.nodes, newDef.metadata);
    }

    public WorkflowDefinition(List<BaseWorkflowNode> nodes, WorkflowMetadata metadata) {
        this.nodes = nodes != null ? nodes :new ArrayList<>();
        this.metadata = metadata != null ? metadata : new WorkflowMetadata();
    }

    public WorkflowDefinition init() {
        List<BaseWorkflowNode> nodes = this.nodes != null ? this.nodes : new ArrayList<>();
        WorkflowMetadata metadata = this.metadata != null ? this.metadata : new WorkflowMetadata();
        return new WorkflowDefinition(nodes, metadata);
    }
}
