package org.phong.zenflow.plugin.subdomain.execution.interfaces;

import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationError;

import java.util.Collections;
import java.util.List;


public interface PluginNodeExecutor {
    // execute the plugin node with the given configuration and context
    ExecutionResult execute(WorkflowConfig config, ExecutionContext context);

    // perform additional definition-time validation on the workflow configuration
    default List<ValidationError> validateDefinition(WorkflowConfig config) {
        return Collections.emptyList();
    }

    // perform additional runtime validation on the resolved configuration
    default List<ValidationError> validateRuntime(WorkflowConfig config, ExecutionContext ctx) {
        return Collections.emptyList();
    }
}
