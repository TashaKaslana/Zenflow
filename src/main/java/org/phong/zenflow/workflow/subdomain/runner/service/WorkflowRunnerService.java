package org.phong.zenflow.workflow.subdomain.runner.service;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.log.auditlog.annotations.AuditLog;
import org.phong.zenflow.log.auditlog.enums.AuditAction;
import org.phong.zenflow.secret.service.SecretService;
import org.phong.zenflow.workflow.subdomain.engine.dto.WorkflowExecutionStatus;
import org.phong.zenflow.workflow.subdomain.engine.service.WorkflowEngineService;
import org.phong.zenflow.workflow.subdomain.runner.dto.WorkflowRunnerRequest;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;
import org.phong.zenflow.workflow.subdomain.workflow_run.infrastructure.persistence.entity.WorkflowRun;
import org.phong.zenflow.workflow.subdomain.workflow_run.service.WorkflowRunService;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
    private final SecretService secretService;
    private static final String callbackUrlKeyOnContext =  "__zenflow_callback_url";

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
        Map<String, Object> context = null;

        try {
            WorkflowRun workflowRun = workflowRunService.findEntityById(workflowRunId);

            if (workflowRun.getContext() == null || workflowRun.getContext().isEmpty()) {
                // First run: ensure the run is started and create a new context
                log.debug("No existing context found for workflow run ID: {}. Starting new run.", workflowRunId);
                workflowRunService.startWorkflowRun(workflowRunId, workflowId, triggerType);
                Map<String, Object> secretOfWorkflow = ObjectConversion.convertObjectToMap(secretService.getSecretsByWorkflowId(workflowId));
                context = new ConcurrentHashMap<>(Map.of("secrets", secretOfWorkflow));
                if (request != null && request.callbackUrl() != null && !request.callbackUrl().isEmpty()) {
                    context.put(callbackUrlKeyOnContext, request.callbackUrl());
                }
            } else {
                // Resumed run: load existing context
                log.debug("Existing context found for workflow run ID: {}. Loading context.", workflowRunId);
                context = new ConcurrentHashMap<>(workflowRun.getContext());
            }

            String startFromNodeKey = (request != null) ? request.startFromNodeKey() : null;
            WorkflowExecutionStatus status = workflowEngineService.runWorkflow(workflowId, workflowRunId, startFromNodeKey, context);

            if (status == WorkflowExecutionStatus.COMPLETED) {
                workflowRunService.completeWorkflowRun(workflowRunId);
                log.debug("Workflow with ID: {} completed successfully", workflowId);
                Object callbackUrlObj = context.get(callbackUrlKeyOnContext);
                if (callbackUrlObj instanceof String callbackUrl && !callbackUrl.isEmpty()) {
                    notifyCallbackUrl(callbackUrl, workflowRunId);
                }
            } else if (status == WorkflowExecutionStatus.HALTED) {
                // Workflow is paused (RETRY or WAITING), save the context for resumption.
                log.debug("Workflow with ID: {} is halted. Saving context.", workflowId);
                workflowRunService.saveContext(workflowRunId, context);
            }

        } catch (Exception e) {
            log.warn("Error running workflow with ID: {}", workflowId, e);
            workflowRunService.handleWorkflowError(workflowRunId, e);
            if (context != null) {
                Object callbackUrlObj = context.get(callbackUrlKeyOnContext);
                if (callbackUrlObj instanceof String callbackUrl && !callbackUrl.isEmpty()) {
                    notifyCallbackUrl(callbackUrl, workflowRunId);
                }
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
