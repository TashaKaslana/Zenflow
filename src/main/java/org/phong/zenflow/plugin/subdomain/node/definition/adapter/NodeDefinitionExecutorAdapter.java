package org.phong.zenflow.plugin.subdomain.node.definition.adapter;

import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeValidator;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationError;

import java.util.Collections;
import java.util.List;

/**
 * Bridges new {@link NodeDefinitionProvider}-based nodes into the existing
 * {@link PluginNodeExecutor} infrastructure until the legacy pathway is removed.
 */
public class NodeDefinitionExecutorAdapter implements PluginNodeExecutor {

    private final NodeDefinitionProvider provider;

    public NodeDefinitionExecutorAdapter(NodeDefinitionProvider provider) {
        this.provider = provider;
    }

    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeDefinition definition = resolveDefinition();
        NodeExecutor executor = definition.getNodeExecutor();
        if (executor == null) {
            throw new IllegalStateException("NodeDefinition must supply a NodeExecutor before execution");
        }
        try {
            return executor.execute(config, context);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Node executor invocation failed", ex);
        }
    }

    @Override
    public List<ValidationError> validateDefinition(WorkflowConfig config) {
        NodeValidator validator = resolveDefinition().getNodeValidator();
        if (validator == null) {
            return Collections.emptyList();
        }
        List<ValidationError> errors = validator.validateDefinition(config);
        return errors != null ? errors : Collections.emptyList();
    }

    @Override
    public List<ValidationError> validateRuntime(WorkflowConfig config, ExecutionContext ctx) {
        NodeValidator validator = resolveDefinition().getNodeValidator();
        if (validator == null) {
            return Collections.emptyList();
        }
        List<ValidationError> errors = validator.validateRuntime(config, ctx);
        return errors != null ? errors : Collections.emptyList();
    }

    private NodeDefinition resolveDefinition() {
        NodeDefinition definition = provider.definition();
        if (definition == null) {
            throw new IllegalStateException("NodeDefinitionProvider returned null definition");
        }
        return definition;
    }
}
