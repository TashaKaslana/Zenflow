package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.database.postgres;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeValidator;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationError;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
public class PostgresSqlNodeValidator implements NodeValidator {
    private final PostgresSqlDefinitionValidator definitionValidator;
    private final PostgresSqlRuntimeValidator runtimeValidator;

    @Override
    public List<ValidationError> validateDefinition(WorkflowConfig config) {
        return definitionValidator.validate(config);
    }

    @Override
    public List<ValidationError> validateRuntime(WorkflowConfig config, ExecutionContext context) {
        return runtimeValidator.validate(config, context);
    }
}
