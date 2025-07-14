package org.phong.zenflow.workflow.subdomain.node_definition.definitions.navigator.loop;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.enums.NodeType;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class BaseLoopDefinition extends BaseWorkflowNode {
    @NotNull
    private final String loopType; //"for", "while"

    @NotNull
    private final List<String> loopEnd;

    public BaseLoopDefinition(String key, NodeType type, List<String> next, Map<String, Object> config,
                              Map<String, Object> metadata, Map<String, Object> policy, String loopType, List<String> loopEnd) {
        super(key, type, next, config, metadata, policy);
        this.loopType = loopType;
        this.loopEnd = loopEnd;
    }
}
