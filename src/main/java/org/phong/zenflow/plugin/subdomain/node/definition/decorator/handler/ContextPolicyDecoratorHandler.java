package org.phong.zenflow.plugin.subdomain.node.definition.decorator.handler;

import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.decorator.ExecutorDecorator;
import org.phong.zenflow.plugin.subdomain.node.definition.policy.ContextAccessPolicy;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.worker.model.ExecutionTaskEnvelope;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

/**
 * Applies the node's context access policy to the execution context for the
 * duration of the node execution.
 */
@Component
public class ContextPolicyDecoratorHandler implements ExecutorDecorator {

    @Override
    public int order() {
        return 50;
    }

    @Override
    public Callable<ExecutionResult> decorate(Callable<ExecutionResult> inner,
                                              NodeDefinition def,
                                              ExecutionTaskEnvelope envelope) {
        ExecutionContext context = envelope.getContext();
        ContextAccessPolicy policy = def.getContextAccessPolicy();
        if (policy == null) {
            policy = ContextAccessPolicy.DEFAULT;
        }

        ContextAccessPolicy finalPolicy = policy;
        return () -> {
            ContextAccessPolicy previous = context.getContextAccessPolicy();
            try {
                context.setContextAccessPolicy(finalPolicy);
                return inner.call();
            } finally {
                context.setContextAccessPolicy(previous != null ? previous : ContextAccessPolicy.DEFAULT);
            }
        };
    }
}
