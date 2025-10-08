package org.phong.zenflow.plugin.subdomain.node.definition.decorator;

import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

@Component
@Slf4j
public class ExecutorPipelineFactory {
    private final List<ExecutorDecorator> decorators;

    public ExecutorPipelineFactory(List<ExecutorDecorator> decorators) {
        this.decorators = decorators.stream()
            .sorted(Comparator.comparingInt(ExecutorDecorator::order))
            .toList();
    }

    public Callable<ExecutionResult> build(NodeDefinition def,
                                           WorkflowConfig cfg,
                                           ExecutionContext ctx) throws Exception {
        if (def.getNodeExecutor() == null) {
            log.error("Node executor is not defined for node type: {}", def.getType());
            throw new Exception("Node executor is not defined for node type: " + def.getType());
        }
        // core = node’s happy path
        Callable<ExecutionResult> pipeline = () -> def.getNodeExecutor().execute(cfg, ctx);

        // wrap in order
        for (ExecutorDecorator decorator : decorators) {
            pipeline = decorator.decorate(pipeline, def, cfg, ctx);
        }

        return pipeline;
    }
}
