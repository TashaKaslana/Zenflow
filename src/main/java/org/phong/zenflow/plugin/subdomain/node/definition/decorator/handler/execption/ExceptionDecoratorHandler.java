package org.phong.zenflow.plugin.subdomain.node.definition.decorator.handler.execption;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.decorator.ExecutorDecorator;
import org.phong.zenflow.workflow.subdomain.worker.model.ExecutionTaskEnvelope;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

@AllArgsConstructor
@Component
public class ExceptionDecoratorHandler implements ExecutorDecorator {
    @Override
    public int order() {
        return 100;
    }

    @Override
    public Callable<ExecutionResult> decorate(Callable<ExecutionResult> inner,
                                              NodeDefinition def,
                                              ExecutionTaskEnvelope envelope) {
        return () -> {
            try {
                return inner.call();
            } catch (Exception e) {
                return ExceptionMapping.mapException(e, envelope.getContext().getLogPublisher());
            }
        };
    }
}
