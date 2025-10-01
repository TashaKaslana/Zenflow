package org.phong.zenflow.plugin.subdomain.node.definition.decorator;

import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;

import java.util.concurrent.Callable;

public interface ExecutorDecorator {
    int order();  // smaller = more outer (runs earlier)

    Callable<ExecutionResult> decorate(Callable<ExecutionResult> inner,
                                       NodeDefinition def,
                                       WorkflowConfig cfg,
                                       ExecutionContext ctx);
}
