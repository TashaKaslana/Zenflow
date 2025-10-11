package org.phong.zenflow.plugin.subdomain.node.definition.decorator.handler.policy;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionError;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.decorator.ExecutorDecorator;
import org.phong.zenflow.workflow.subdomain.worker.model.ExecutionTaskEnvelope;
import org.phong.zenflow.workflow.subdomain.worker.policy.ExecutionPolicyResolver;
import org.phong.zenflow.workflow.subdomain.worker.policy.ResolvedExecutionPolicy;
import org.springframework.stereotype.Component;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.retry.MaxRetriesExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResiliencePolicyDecorator implements ExecutorDecorator {

    private final ExecutionPolicyResolver policyResolver;
    private final List<ResiliencePolicyHandler> policyHandlers;

    @Override
    public int order() {
        return 500;
    }

    @Override
    public Callable<ExecutionResult> decorate(Callable<ExecutionResult> inner,
                                              NodeDefinition def,
                                              ExecutionTaskEnvelope envelope) {
        ResolvedExecutionPolicy policy = policyResolver.resolve(def, envelope.getConfig());
        if (policy.isEmpty()) {
            return inner;
        }

        String policyKey = resolvePolicyKey(envelope, def);

        Callable<ExecutionResult> wrapped = inner;
        for (ResiliencePolicyHandler policyHandler : policyHandlers) {
            wrapped = policyHandler.apply(wrapped, policy, policyKey, envelope);
        }

        Callable<ExecutionResult> finalWrapped = wrapped;
        return getAppliedPolicyWrapper(finalWrapped, policy, policyKey);
    }

    private Callable<ExecutionResult> getAppliedPolicyWrapper(Callable<ExecutionResult> finalWrapped, ResolvedExecutionPolicy policy, String policyKey) {
        return () -> {
            try {
                return finalWrapped.call();
            } catch (TimeoutException e) {
                String message = "Execution timed out after " + policy.getTimeout();
                logPolicyWarning(policyKey, message);
                return ExecutionResult.error(ExecutionError.TIMEOUT, message);
            } catch (RequestNotPermitted e) {
                String message = "Rate limit exceeded for node execution";
                logPolicyWarning(policyKey, message);
                return ExecutionResult.error(ExecutionError.RETRIABLE, message);
            } catch (MaxRetriesExceededException e) {
                String message = "Maximum retry attempts exhausted";
                logPolicyWarning(policyKey, message, e);
                return ExecutionResult.error(ExecutionError.RETRIABLE, message);
            } catch (CompletionException e) {
                Throwable cause = Optional.ofNullable(e.getCause()).orElse(e);
                if (cause instanceof TimeoutException) {
                    String message = "Execution timed out after " + policy.getTimeout();
                    logPolicyWarning(policyKey, message);
                    return ExecutionResult.error(ExecutionError.TIMEOUT, message);
                }
                if (cause instanceof RequestNotPermitted) {
                    String message = "Rate limit exceeded for node execution";
                    logPolicyWarning(policyKey, message);
                    return ExecutionResult.error(ExecutionError.RETRIABLE, message);
                }
                throw cause instanceof Exception ? (Exception) cause : new RuntimeException(cause);
            }
        };
    }

    private String resolvePolicyKey(ExecutionTaskEnvelope envelope, NodeDefinition definition) {
        if (envelope.getPluginNodeId() != null) {
            return envelope.getPluginNodeId().toString();
        }
        if (definition != null && definition.getName() != null) {
            return definition.getName();
        }
        return "node";
    }

    private void logPolicyWarning(String policyKey, String message) {
        log.warn("[policyKey={}] {}", policyKey, message);
    }

    private void logPolicyWarning(String policyKey, String message, Throwable throwable) {
        log.warn("[policyKey={}] {}", policyKey, message, throwable);
    }
}
