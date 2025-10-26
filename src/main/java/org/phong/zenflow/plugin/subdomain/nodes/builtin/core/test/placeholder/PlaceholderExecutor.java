package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.test.placeholder;

import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PlaceholderExecutor implements NodeExecutor {
    @Override
    public ExecutionResult execute(ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();
        Map<String, Object> input = context.getCurrentConfig().input();
        input.forEach((k, v) -> {
            logCollector.info("Input {}: {}", k, v);
            context.write(k, v);
        });
        return ExecutionResult.success();
    }
}
