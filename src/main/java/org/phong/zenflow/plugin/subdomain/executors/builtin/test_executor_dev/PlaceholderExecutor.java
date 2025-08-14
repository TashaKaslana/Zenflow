package org.phong.zenflow.plugin.subdomain.executors.builtin.test_executor_dev;

import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_logs.utils.LogCollector;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@PluginNode(
        key = "core:placeholder",
        name = "Placeholder Node",
        version = "1.0.0",
        description = "A placeholder node that echoes its input as output.",
        icon = "ph:placeholder",
        tags = {"data", "placeholder"},
        type = "data"
)
public class PlaceholderExecutor implements PluginNodeExecutor {

    @Override
    public String key() {
        return "core:placeholder:1.0.0";
    }

    @Override
    public ExecutionResult execute(WorkflowConfig config, RuntimeContext context) {
        LogCollector logCollector = new LogCollector();
        Map<String, Object> input = ObjectConversion.convertObjectToMap(config.input());
        input.forEach((k, v) -> logCollector.info("Input {}: {}", k, v));
        return ExecutionResult.success(input, logCollector.getLogs());
    }
}
