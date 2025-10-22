package org.phong.zenflow.plugin.subdomain.node.definition.decorator;

import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.workflow.subdomain.worker.model.ExecutionTaskEnvelope;
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
                                           ExecutionTaskEnvelope envelope) throws Exception {
        if (def.getNodeExecutor() == null) {
            log.error("Node executor is not defined for node type: {}", def.getType());
            throw new Exception("Node executor is not defined for node type: " + def.getType());
        }
        var ctx = envelope.getContext();
        // core = node's happy path
        Callable<ExecutionResult> pipeline = () -> def.getNodeExecutor().execute(ctx);

        // wrap in order
        for (ExecutorDecorator decorator : decorators) {
            pipeline = decorator.decorate(pipeline, def, envelope);
        }

        return pipeline;
    }
}
