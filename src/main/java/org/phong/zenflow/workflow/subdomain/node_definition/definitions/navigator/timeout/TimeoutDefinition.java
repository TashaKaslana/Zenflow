package org.phong.zenflow.workflow.subdomain.node_definition.definitions.navigator.timeout;

import lombok.Getter;
import lombok.Setter;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.enums.NodeType;

import java.util.List;
import java.util.Map;

@Setter
@Getter
public final class TimeoutDefinition extends BaseWorkflowNode {
    private final String duration;
    //supports: millisecond, seconds
    private final String unit;

    public TimeoutDefinition(String key, NodeType type, List<String> next, Map<String, Object> config,
                             Map<String, Object> metadata, Map<String, Object> policy, String duration, String unit) {
        super(key, type, next, config, metadata, policy);
        this.duration = duration;
        this.unit = unit;
    }
}
