package org.phong.zenflow.workflow.subdomain.node_definition.definitions.trigger;

import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.NodeExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TriggerNodeExecutor implements NodeExecutor<TriggerNodeDefinition> {
    @Override
    public String getNodeType() {
        return "TRIGGER";
    }

    @Override
    public ExecutionResult execute(TriggerNodeDefinition node, Map<String, Object> context) {
        return ExecutionResult.nextNode(node.getNext().getFirst());
    }
}
