package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.executor;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Convenience base class for executing a single transformer without requiring
 * the caller to specify the transformer name in the configuration.
 */
@AllArgsConstructor
public abstract class AbstractSingleTransformExecutor implements PluginNodeExecutor {
    private final DataTransformerExecutor delegate;

    protected abstract String transformerName();

    @Override
    public ExecutionResult execute(WorkflowConfig config, RuntimeContext context) {
        Map<String, Object> input = new HashMap<>(config.input());
        input.put("name", transformerName());
        WorkflowConfig updated = new WorkflowConfig(input, config.output());
        return delegate.execute(updated, context);
    }
}
