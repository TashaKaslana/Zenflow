package org.phong.zenflow.workflow.subdomain.runner.service;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.log.auditlog.annotations.AuditLog;
import org.phong.zenflow.log.auditlog.enums.AuditAction;
import org.phong.zenflow.workflow.subdomain.engine.dto.WorkflowExecutionStatus;
import org.phong.zenflow.workflow.subdomain.engine.service.WorkflowEngineService;
import org.phong.zenflow.workflow.subdomain.runner.dto.WorkflowRunnerRequest;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;
import org.phong.zenflow.workflow.subdomain.workflow_run.service.WorkflowRunService;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;
import org.phong.zenflow.workflow.service.WorkflowService;
import org.phong.zenflow.workflow.exception.WorkflowException;

@AllArgsConstructor
@Slf4j
@Service
public class WorkflowRunnerService {
    private final WorkflowEngineService workflowEngineService;
    private final WorkflowRunService workflowRunService;
    private final WebClient webClient;
    private final WorkflowService workflowService;

    @AuditLog(
            action = AuditAction.WORKFLOW_EXECUTE,
            description = "Run a workflow with the given ID",
            targetIdExpression = "#workflowId"
    )
    public void runWorkflow(UUID workflowRunId, TriggerType triggerType, UUID workflowId, @Nullable WorkflowRunnerRequest request) {
        log.debug("Starting workflow with ID: {}", workflowId);

        if (!workflowService.findById(workflowId).isActive()) {
            throw new WorkflowException("Workflow with ID: " + workflowId + " is not active");
        }

        triggerType = triggerType != null ? triggerType : TriggerType.MANUAL;
        boolean isNotifyByWebhook = request != null && !request.callbackUrl().isEmpty();
        try {
            workflowRunService.startWorkflowRun(workflowRunId, workflowId, triggerType);
            WorkflowExecutionStatus status = workflowEngineService.runWorkflow(workflowId, workflowRunId, null);

            if (status == WorkflowExecutionStatus.COMPLETED) {
                workflowRunService.completeWorkflowRun(workflowRunId);
                log.debug("Workflow with ID: {} completed successfully", workflowId);
                if (isNotifyByWebhook) {
                    notifyCallbackUrl(request.callbackUrl(), workflowRunId);
                }
            }
            // If status is HALTED, we do nothing. The workflow run remains in RUNNING state.

        } catch (Exception e) {
            log.warn("Error running workflow with ID: {}", workflowId, e);
            workflowRunService.handleWorkflowError(workflowRunId, e);
            if (isNotifyByWebhook) {
                notifyCallbackUrl(request.callbackUrl(), workflowRunId);
            }
        }
    }

    private void notifyCallbackUrl(@NotNull @NotEmpty String callbackUrl, UUID workflowRunId) {
        if (callbackUrl != null && !callbackUrl.isEmpty()) {
            webClient.post()
                    .uri(callbackUrl)
                    .bodyValue(workflowRunId)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .doOnError(error -> log.error("Failed to notify callback URL: {}", callbackUrl, error))
                    .subscribe();
        }
    }
}
