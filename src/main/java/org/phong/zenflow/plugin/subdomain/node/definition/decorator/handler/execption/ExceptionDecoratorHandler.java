package org.phong.zenflow.plugin.subdomain.node.definition.decorator.handler.execption;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.decorator.ExecutorDecorator;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

@AllArgsConstructor
@Component
public class ExceptionDecoratorHandler implements ExecutorDecorator {
    @Override
    public int order() {
        return 0;
    }

    @Override
    public Callable<ExecutionResult> decorate(Callable<ExecutionResult> inner,
                                              NodeDefinition def,
                                              WorkflowConfig cfg,
                                              ExecutionContext ctx) {
        return () -> {
            try {
                return inner.call();
            } catch (Exception e) {
                return ExceptionMapping.mapException(e, ctx.getLogPublisher());
            }
        };
    }
}
