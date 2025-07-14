package org.phong.zenflow.workflow.subdomain.node_definition.definitions.navigator.condition.executor;

import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.navigator.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.navigator.condition.SwitchCaseDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.navigator.condition.SwitchDefinition;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class SwitchNodeExecutor implements NodeExecutor<SwitchDefinition> {
    @Override
    public ExecutionResult execute(SwitchDefinition node, Map<String, Object> context) {
        Object value = context.get(node.getCompare());
        if (value == null) {
            log.warn("Switch compare value not found in context");
            return ExecutionResult.nextNode("default_case");
        }
        for (SwitchCaseDefinition c : node.getCases()) {
            if (c.value().equals(value)) {
                return ExecutionResult.nextNode(c.next());
            }
        }
        return ExecutionResult.nextNode(node.getDefaultCase());
    }
}
