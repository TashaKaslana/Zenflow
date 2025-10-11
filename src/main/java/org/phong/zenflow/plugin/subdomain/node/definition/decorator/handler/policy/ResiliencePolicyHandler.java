package org.phong.zenflow.plugin.subdomain.node.definition.decorator.handler.policy;

import java.util.concurrent.Callable;

import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.workflow.subdomain.worker.model.ExecutionTaskEnvelope;
import org.phong.zenflow.workflow.subdomain.worker.policy.ResolvedExecutionPolicy;

/**
 * Applies a specific resilience policy concern to the node execution callable.
 */
public interface ResiliencePolicyHandler {

    Callable<ExecutionResult> apply(Callable<ExecutionResult> callable,
                                    ResolvedExecutionPolicy policy,
                                    String policyKey,
                                    ExecutionTaskEnvelope envelope);
}
