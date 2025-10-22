package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.executor;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Convenience base class for executing a single transformer without requiring
 * the caller to specify the transformer name in the configuration.
 */
@AllArgsConstructor
public abstract class AbstractSingleTransformExecutor implements NodeExecutor {
    private final DataTransformerExecutor delegate;

    protected abstract String transformerName();

    @Override
    public ExecutionResult execute(ExecutionContext context) throws Exception {
        WorkflowConfig config = context.getCurrentConfig();
        if (config == null) {
            config = new WorkflowConfig();
        }

        Map<String, Object> input = new HashMap<>(config.input());
        input.put("name", transformerName());
        WorkflowConfig updated = new WorkflowConfig(input, config.output());

        WorkflowConfig previous = context.getCurrentConfig();
        context.setCurrentConfig(updated);
        try {
            return delegate.execute(context);
        } finally {
            context.setCurrentConfig(previous);
        }
    }
}
