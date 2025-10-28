package org.phong.zenflow.workflow.subdomain.policy;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionError;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.plugin.subdomain.execution.services.NodeExecutorDispatcher;
import org.phong.zenflow.plugin.subdomain.node.definition.decorator.handler.policy.RateLimitPolicyHandler;
import org.phong.zenflow.plugin.subdomain.node.definition.decorator.handler.policy.ResiliencePolicyDecorator;
import org.phong.zenflow.plugin.subdomain.node.definition.decorator.handler.policy.RetryPolicyHandler;
import org.phong.zenflow.plugin.subdomain.node.definition.decorator.handler.policy.TimeoutPolicyHandler;
import org.phong.zenflow.plugin.subdomain.node.definition.policy.NodeExecutionPolicy;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContextManager;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.worker.ExecutionTaskRegistry;
import org.phong.zenflow.workflow.subdomain.worker.gateway.ExecutionGatewayImpl;
import org.phong.zenflow.workflow.subdomain.worker.model.ExecutionTaskEnvelope;
import org.phong.zenflow.workflow.subdomain.worker.policy.ExecutionPolicyResolver;
import org.phong.zenflow.workflow.subdomain.worker.policy.ResolvedExecutionPolicy;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

            ExecutionGatewayImpl gateway = getExecutionGateway(resolver, decoratorExecutor, taskExecutor);

            ExecutionTaskEnvelope envelope = ExecutionTaskEnvelope.builder()
                    .taskId("task-123")
                    .executorIdentifier("any")
                    .executorType("builtin")
                    .config(Mockito.mock(WorkflowConfig.class))
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

    @Test
    void retryPolicyRetriesWhenResultRequestsRetry() throws Exception {
        ExecutionPolicyResolver resolver = Mockito.mock(ExecutionPolicyResolver.class);
        ResolvedExecutionPolicy policy = ResolvedExecutionPolicy.builder()
                .retry(NodeExecutionPolicy.RetryPolicy.builder()
                        .maxAttempts(3)
                        .waitDuration(Duration.ZERO)
                        .build())
                .build();
        Mockito.when(resolver.resolve(Mockito.any(), Mockito.any()))
                .thenReturn(policy);

        RuntimeContextManager contextManager = Mockito.mock(RuntimeContextManager.class);
        
        ResiliencePolicyDecorator decorator = new ResiliencePolicyDecorator(resolver, List.of(
                new RetryPolicyHandler(contextManager)
        ));

        ExecutionTaskEnvelope envelope = ExecutionTaskEnvelope.builder()
                .taskId("retry-task")
                .executorIdentifier("any")
                .executorType("builtin")
                .config(Mockito.mock(WorkflowConfig.class))
                .pluginNodeId(UUID.randomUUID())
                .build();

        AtomicInteger attempts = new AtomicInteger();
        Callable<ExecutionResult> inner = () -> {
            int attempt = attempts.incrementAndGet();
            if (attempt < 3) {
                return ExecutionResult.retry("try again");
            }
            return ExecutionResult.success();
        };

        ExecutionResult result = decorator.decorate(inner, null, envelope).call();

        assertThat(result.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    void rateLimitPolicyReturnsRetriableErrorWhenLimitExceeded() throws Exception {
        ExecutionPolicyResolver resolver = Mockito.mock(ExecutionPolicyResolver.class);
        ResolvedExecutionPolicy policy = ResolvedExecutionPolicy.builder()
                .rateLimit(NodeExecutionPolicy.RateLimitPolicy.builder()
                        .limitForPeriod(1)
                        .refreshPeriod(Duration.ofSeconds(60))
                        .timeoutDuration(Duration.ZERO)
                        .build())
                .build();
        Mockito.when(resolver.resolve(Mockito.any(), Mockito.any()))
                .thenReturn(policy);

        ResiliencePolicyDecorator decorator = new ResiliencePolicyDecorator(resolver, List.of(
                new RateLimitPolicyHandler()
        ));

        ExecutionTaskEnvelope envelope = ExecutionTaskEnvelope.builder()
                .taskId("rate-limit-task")
                .executorIdentifier("any")
                .executorType("builtin")
                .config(Mockito.mock(WorkflowConfig.class))
                .pluginNodeId(UUID.randomUUID())
                .build();

        Callable<ExecutionResult> inner = () -> ExecutionResult.success();
        Callable<ExecutionResult> decorated = decorator.decorate(inner, null, envelope);

        ExecutionResult first = decorated.call();
        ExecutionResult second = decorated.call();

        assertThat(first.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
        assertThat(second.getStatus()).isEqualTo(ExecutionStatus.ERROR);
        assertThat(second.getErrorType()).isEqualTo(ExecutionError.RETRIABLE);
        assertThat(second.getError()).contains("Rate limit exceeded");
    }

    private static @NotNull ExecutionGatewayImpl getExecutionGateway(ExecutionPolicyResolver resolver, ExecutorService decoratorExecutor, ExecutorService taskExecutor) {
        RuntimeContextManager contextManager = Mockito.mock(RuntimeContextManager.class);
        
        ResiliencePolicyDecorator decorator = new ResiliencePolicyDecorator(resolver, List.of(
                new RateLimitPolicyHandler(),
                new RetryPolicyHandler(contextManager),
                new TimeoutPolicyHandler(decoratorExecutor)
        ));

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

        return new ExecutionGatewayImpl(
                new ExecutionTaskRegistry(),
                dispatcher,
                taskExecutor);
    }
}
