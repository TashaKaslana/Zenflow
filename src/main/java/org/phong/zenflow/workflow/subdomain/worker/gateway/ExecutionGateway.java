package org.phong.zenflow.workflow.subdomain.worker.gateway;

import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.workflow.subdomain.worker.model.ExecutionTaskEnvelope;

import java.util.concurrent.CompletableFuture;

public interface ExecutionGateway {
    CompletableFuture<ExecutionResult> executeAsync(ExecutionTaskEnvelope envelope);

    boolean cancelAsync(ExecutionTaskEnvelope envelope);
}
