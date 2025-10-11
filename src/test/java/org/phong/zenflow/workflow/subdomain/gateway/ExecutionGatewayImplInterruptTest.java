package org.phong.zenflow.workflow.subdomain.gateway;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionError;
import org.phong.zenflow.plugin.subdomain.execution.services.NodeExecutorDispatcher;
import org.phong.zenflow.plugin.subdomain.node.definition.decorator.handler.policy.ResiliencePolicyDecorator;
import org.phong.zenflow.workflow.subdomain.worker.ExecutionTaskRegistry;
import org.phong.zenflow.workflow.subdomain.worker.gateway.ExecutionGatewayImpl;
import org.phong.zenflow.workflow.subdomain.worker.model.ExecutionTaskEnvelope;
import org.phong.zenflow.workflow.subdomain.worker.policy.ExecutionPolicyResolver;
import org.phong.zenflow.workflow.subdomain.worker.policy.ResolvedExecutionPolicy;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionGatewayImplInterruptTest {

    @Test
    void handlesCompletionExceptionFromResilienceDecorator() throws Exception {
        ExecutorService decoratorExecutor = Executors.newSingleThreadExecutor();
        ExecutorService taskExecutor = Executors.newSingleThreadExecutor();
        try {
            ExecutionPolicyResolver resolver = Mockito.mock(ExecutionPolicyResolver.class);
            ResolvedExecutionPolicy policy = ResolvedExecutionPolicy.builder()
                    .timeout(Duration.ofMillis(100))
                    .build();
            Mockito.when(resolver.resolve(Mockito.any(), Mockito.any()))
                    .thenReturn(policy);

            ResiliencePolicyDecorator decorator = new ResiliencePolicyDecorator(resolver, decoratorExecutor);

            NodeExecutorDispatcher dispatcher = new NodeExecutorDispatcher(null, null) {
                @Override
                public ExecutionResult dispatch(ExecutionTaskEnvelope envelope) {
                    Callable<ExecutionResult> inner = () -> {
                        throw new InterruptedException("boom");
                    };
                    try {
                        return decorator.decorate(inner, null, envelope).call();
                    } catch (RuntimeException runtimeException) {
                        throw runtimeException;
                    } catch (Exception exception) {
                        throw new RuntimeException(exception);
                    }
                }
            };

            ExecutionGatewayImpl gateway = new ExecutionGatewayImpl(
                    new ExecutionTaskRegistry(),
                    dispatcher,
                    taskExecutor);

            ExecutionTaskEnvelope envelope = ExecutionTaskEnvelope.builder()
                    .taskId("task-123")
                    .executorIdentifier("any")
                    .executorType("builtin")
                    .config(Mockito.mock(org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig.class))
                    .build();

            CompletableFuture<ExecutionResult> future = gateway.executeAsync(envelope);
            ExecutionResult result = future.get(1, TimeUnit.SECONDS);

            assertThat(result.getErrorType()).isEqualTo(ExecutionError.INTERRUPTED);
            assertThat(result.getError()).contains("Execution interrupted");
        } finally {
            decoratorExecutor.shutdownNow();
            taskExecutor.shutdownNow();
        }
    }
}
