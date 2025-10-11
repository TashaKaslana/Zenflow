package org.phong.zenflow.plugin.subdomain.node.definition.decorator.handler.validation;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.decorator.ExecutorDecorator;
import org.phong.zenflow.workflow.subdomain.worker.model.ExecutionTaskEnvelope;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationResult;
import org.phong.zenflow.workflow.subdomain.schema_validator.service.WorkflowValidationService;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

@Component
@AllArgsConstructor
@Slf4j
public class ValidationDecoratorHandler implements ExecutorDecorator {
    private final WorkflowValidationService workflowValidationService;

    @Override
    public int order() {
        return 0;
    }

    @Override
    public Callable<ExecutionResult> decorate(Callable<ExecutionResult> inner,
                                              NodeDefinition def,
                                              ExecutionTaskEnvelope envelope) {
        if (def.getNodeValidator() == null || envelope.getPluginNodeId() == null) {
            return inner;
        }

        return () -> {
            ValidationResult validationResult = workflowValidationService.validateRuntime(
                    envelope.getContext().getNodeKey(),
                    envelope.getConfig(),
                    envelope.getPluginNodeId().toString(),
                    envelope.getContext()
            );

            if (!validationResult.isValid()) {
                log.warn("Validation failed for node {}, plugin node id {}: {}",
                        envelope.getContext().getNodeKey(), envelope.getPluginNodeId(), validationResult.getErrors()
                );
                return ExecutionResult.validationError(validationResult, envelope.getContext().getNodeKey());
            }

            return inner.call();
        };
    }
}
