package org.phong.zenflow.plugin.subdomain.node.definition.decorator.handler.resource;

import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.decorator.ExecutorDecorator;
import org.phong.zenflow.plugin.subdomain.resource.BaseNodeResourceManager;
import org.phong.zenflow.plugin.subdomain.resource.ScopedNodeResource;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

@Component
public class ResourceDecoratorHandler implements ExecutorDecorator {
    @Override
    public int order() { return 300; }

    @Override
    public Callable<ExecutionResult> decorate(Callable<ExecutionResult> inner,
                                              NodeDefinition def,
                                              WorkflowConfig cfg,
                                              ExecutionContext ctx) {
        BaseNodeResourceManager<?, ?> nodeResourceManager = def.getNodeResourceManager();
        if (nodeResourceManager == null || nodeResourceManager.isManual()) {
            return inner;
        }

        return () -> {
            try (ScopedNodeResource<?> h = nodeResourceManager.acquire(cfg, ctx)) {
                ctx.setScopedResource(h);
                return inner.call();
            }
        };
    }
}
