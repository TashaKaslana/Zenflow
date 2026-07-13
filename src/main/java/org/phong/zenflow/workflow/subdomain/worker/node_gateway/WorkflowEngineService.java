package org.phong.zenflow.workflow.subdomain.worker.node_gateway;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContextKey;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContextManager;
import org.phong.zenflow.workflow.subdomain.engine.service.WorkflowNavigatorService;
import org.phong.zenflow.workflow.subdomain.logging.core.LogContextManager;
import org.phong.zenflow.workflow.subdomain.worker.ExecutionTaskRegistry;
import org.phong.zenflow.workflow.subdomain.worker.model.ExecutionTaskEnvelope;
import org.phong.zenflow.workflow.subdomain.worker.node_gateway.enums.WorkflowExecutionStatus;
import org.phong.zenflow.workflow.subdomain.worker.node_gateway.event.WorkflowFinished;
import org.phong.zenflow.workflow.subdomain.worker.node_gateway.event.WorkflowNodeFinished;
import org.phong.zenflow.workflow.subdomain.worker.node_gateway.event.WorkflowNodeStart;
import org.phong.zenflow.workflow.subdomain.workflow_run.service.WorkflowRunService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;
import java.util.concurrent.Executor;


@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowEngineService {
    private final ApplicationEventPublisher eventPublisher;
    private final ExecutionTaskRegistry taskRegistry;
    private final WorkflowRunService workflowRunService;
    @Qualifier("virtualThreadExecutor")
    private final Executor executor;
    private final WebClient webClient;
    private final RuntimeContextManager contextManager;

    public void processWorkflow(WorkflowNodeFinished event) {
        ExecutionTaskEnvelope taskEnvelope = taskRegistry.getTaskEnvelope(event.getTaskId());
        ExecutionContext context = taskEnvelope.getContext();

        String nextNodeKey = event.getExecutionResult().getNextNodeKey();

        if (nextNodeKey == null) {
            eventPublisher.publishEvent(new WorkflowFinished(
                    event.getWorkflowRunId(),
                    event.getStatus() == WorkflowExecutionStatus.PROCESSING
                            ? WorkflowExecutionStatus.PROCESSING : WorkflowExecutionStatus.COMPLETED,
                    event.getMessage()
            ));
        }

        //not save the status or result per node to db at the moment

        eventPublisher.publishEvent(new WorkflowNodeStart(
                event.getWorkflowRunId(),
                context.getNodeKey()
        ));
    }

    public void processWorkflowFinishedState(WorkflowFinished event) {
        ExecutionTaskEnvelope taskEnvelope = taskRegistry.getTaskEnvelope(event.getTaskId());
        ExecutionContext context = taskEnvelope.getContext();

        handleWorkflowExecutionStatus(
                context.getWorkflowRunId(),
                context.getWorkflowId(),
                event.getStatus(),
                (RuntimeContext) contextManager.get(context.getWorkflowRunId().toString()),
                event.getMessage()
        );
    }

    private void handleWorkflowExecutionStatus(UUID workflowRunId,
                                               UUID workflowId,
                                               WorkflowExecutionStatus status,
                                               RuntimeContext context,
                                               String message) {
        if (status == WorkflowExecutionStatus.COMPLETED) {
            workflowRunService.completeWorkflowRun(workflowRunId);
            log.debug("Workflow with ID: {} completed successfully", workflowId);

            Object callbackUrlObj = context.get(ExecutionContextKey.CALLBACK_URL.key());
            if (callbackUrlObj instanceof String callbackUrl && !callbackUrl.isEmpty()) {
                notifyCallbackUrl(callbackUrl, workflowRunId);
            }
        } else if (status == WorkflowExecutionStatus.PROCESSING) {
            // Workflow is paused (RETRY or WAITING), save the context for resumption.
            log.debug("Workflow with ID: {} is halted. Saving context.", workflowId);
            workflowRunService.saveContext(workflowRunId, context.getContext());
        } else if (status == WorkflowExecutionStatus.ERROR) {
            log.warn("Error running workflow with ID: {}", workflowId);
            workflowRunService.handleWorkflowError(workflowRunId, message);
        }
    }

    private void notifyCallbackUrl(@NotNull @NotEmpty String callbackUrl, UUID workflowRunId) {
        if (callbackUrl != null && !callbackUrl.isEmpty()) {
            executor.execute(() -> {
                String runId = workflowRunId.toString();
                String traceId = UUID.randomUUID().toString();
                LogContextManager.init(runId, traceId);

                try {
                    LogContextManager.withContext(runId, () -> {
                        webClient.post()
                                .uri(callbackUrl)
                                .bodyValue(workflowRunId)
                                .retrieve()
                                .bodyToMono(Void.class)
                                .doOnError(error -> log.error("Failed to notify callback URL: {}", callbackUrl, error))
                                .subscribe();
                        return null;
                    });
                } finally {
                    LogContextManager.cleanup(runId);
                }
            });
        }
    }
}
