package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.test.placeholder;

import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PlaceholderExecutor implements NodeExecutor {
    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();
        Map<String, Object> input = ObjectConversion.convertObjectToMap(config.input());
        input.forEach((k, v) -> logCollector.info("Input {}: {}", k, v));
        return ExecutionResult.success(input);
    }
}
