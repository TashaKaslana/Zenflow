package org.phong.zenflow.plugin.subdomain.node.definition.aspect;

import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationError;

import java.util.List;

public interface NodeValidator {
    List<ValidationError> validateDefinition(WorkflowConfig config);
    List<ValidationError> validateRuntime(WorkflowConfig config, ExecutionContext context);
}
