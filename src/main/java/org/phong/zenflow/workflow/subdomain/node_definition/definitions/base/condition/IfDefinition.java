package org.phong.zenflow.workflow.subdomain.node_definition.definitions.base.condition;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.enums.NodeType;

import java.util.List;
import java.util.Map;

@Setter
@Getter
public final class IfDefinition extends BaseWorkflowNode {
    @NotNull
    private final String condition;

    @NotNull
    private final List<String> nextTrue;

    @NotNull
    private final List<String> nextFalse;

    public IfDefinition(String key, NodeType type, List<String> next, Map<String, Object> config,
                        Map<String, Object> metadata, Map<String, Object> policy, String condition,
                        List<String> nextTrue, List<String> nextFalse) {
        super(key, type, next, config, metadata, policy);
        this.condition = condition;
        this.nextTrue = nextTrue;
        this.nextFalse = nextFalse;
    }
}
