package org.phong.zenflow.plugin.subdomain.node.definition.decorator.handler.resource;

import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.decorator.ExecutorDecorator;
import org.phong.zenflow.plugin.subdomain.resource.BaseNodeResourceManager;
import org.phong.zenflow.plugin.subdomain.resource.ScopedNodeResource;
import org.phong.zenflow.workflow.subdomain.worker.model.ExecutionTaskEnvelope;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

@Component
public class ResourceDecoratorHandler implements ExecutorDecorator {
    @Override
    public int order() { return 300; }

    @Override
    public Callable<ExecutionResult> decorate(Callable<ExecutionResult> inner,
                                              NodeDefinition def,
                                              ExecutionTaskEnvelope envelope) {
        BaseNodeResourceManager<?, ?> nodeResourceManager = def.getNodeResourceManager();
        if (nodeResourceManager == null) {
            return inner;
        }

        if (nodeResourceManager.isManual() || !def.shouldAutoAcquireResource()) {
            return inner;
        }

        return () -> {
            try (ScopedNodeResource<?> h = nodeResourceManager.acquire(envelope.getConfig(), envelope.getContext())) {
                envelope.getContext().setScopedResource(h);
                return inner.call();
            }
        };
    }
}
