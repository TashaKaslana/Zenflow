package org.phong.zenflow.workflow.subdomain.node_definition.definitions;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public record WorkflowDefinition(List<BaseWorkflowNode> nodes, Map<String, Object> metadata) implements Serializable {
    public WorkflowDefinition() {
        this(List.of(), Map.of());
    }

    public WorkflowDefinition(WorkflowDefinition newDef) {
        this(newDef.nodes, newDef.metadata);
    }
}
