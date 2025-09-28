package org.phong.zenflow.workflow.subdomain.node_definition.definitions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowMetadata;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;

@Component
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowDefinition(WorkflowNodes nodes, WorkflowMetadata metadata) implements Serializable {
    public WorkflowDefinition() {
        this(new WorkflowNodes(), new WorkflowMetadata());
    }

    public WorkflowDefinition(WorkflowDefinition existingDef) {
        this(existingDef.nodes(), existingDef.metadata());
    }

    public WorkflowDefinition(List<BaseWorkflowNode> nodes, WorkflowMetadata metadata) {
        this(new WorkflowNodes(nodes), metadata);
    }

    public WorkflowDefinition(WorkflowNodes nodes, WorkflowMetadata metadata) {
        this.nodes = nodes != null ? nodes : new WorkflowNodes();
        this.metadata = metadata != null ? metadata : new WorkflowMetadata();
    }

    /**
     * Creates a deep copy using manual copying (the fastest approach - 10-50x faster than Jackson)
     */
    public WorkflowDefinition deepCopy() {
        WorkflowNodes copiedNodes = nodes != null ? nodes.deepCopy() : new WorkflowNodes();
        WorkflowMetadata copiedMetadata = metadata != null ?
            new WorkflowMetadata(metadata) :
            new WorkflowMetadata();

        return new WorkflowDefinition(copiedNodes, copiedMetadata);
    }
}
