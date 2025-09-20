package org.phong.zenflow.workflow.subdomain.runner.service;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.log.auditlog.annotations.AuditLog;
import org.phong.zenflow.log.auditlog.enums.AuditAction;
import org.phong.zenflow.core.utils.MapUtils;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.secret.dto.AggregatedSecretSetupDto;
import org.phong.zenflow.secret.subdomain.aggregate.SecretAggregateService;
import org.phong.zenflow.workflow.exception.WorkflowException;
import org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow;
import org.phong.zenflow.workflow.service.WorkflowService;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContextKey;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContextManager;
import org.phong.zenflow.workflow.subdomain.engine.dto.WorkflowExecutionStatus;
import org.phong.zenflow.workflow.subdomain.engine.service.WorkflowEngineService;
import org.phong.zenflow.workflow.subdomain.logging.core.LogContextManager;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowNodes;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowMetadata;
import org.phong.zenflow.workflow.subdomain.runner.dto.WorkflowRunnerRequest;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;
import org.phong.zenflow.workflow.subdomain.workflow_run.infrastructure.persistence.entity.WorkflowRun;
import org.phong.zenflow.workflow.subdomain.workflow_run.service.WorkflowRunService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WorkflowRunnerService {
    private final WorkflowEngineService workflowEngineService;
    private final WorkflowRunService workflowRunService;
    private final WebClient webClient;
    private final WorkflowService workflowService;
    private final SecretAggregateService secretAggregateService;
    private final Executor executor;
    private final RuntimeContextManager contextManager;

    public WorkflowRunnerService(
            WorkflowEngineService workflowEngineService,
            WorkflowRunService workflowRunService,
            WebClient webClient,
            WorkflowService workflowService,
            SecretAggregateService secretAggregateService,
            @Qualifier("virtualThreadExecutor") Executor executor,
            RuntimeContextManager contextManager
    ) {
        this.workflowEngineService = workflowEngineService;
        this.workflowRunService = workflowRunService;
        this.webClient = webClient;
        this.workflowService = workflowService;
        this.secretAggregateService = secretAggregateService;
        this.executor = executor;
        this.contextManager = contextManager;
    }

    @AuditLog(
            action = AuditAction.WORKFLOW_EXECUTE,
            description = "Run a workflow with the given ID",
            targetIdExpression = "#workflowId"
    )
    public void runWorkflow(UUID workflowRunId,
                            TriggerType triggerType,
                            @Nullable UUID triggerExecutorId,
                            UUID workflowId,
                            @Nullable WorkflowRunnerRequest request) {
        // Initialize logging context with trace ID for this workflow run
        String traceId = UUID.randomUUID().toString();
        LogContextManager.init(workflowRunId.toString(), traceId);

        try {
            log.info("Starting workflow with ID: {}", workflowId);

            Workflow workflow = workflowService.getWorkflow(workflowId);
            if (!workflow.getIsActive()) {
                throw new WorkflowException("Workflow with ID: " + workflowId + " is not active");
            }

            triggerType = triggerType != null ? triggerType : TriggerType.MANUAL;
            RuntimeContext context = new RuntimeContext();
            contextManager.assign(workflowRunId.toString(), context);

            processWorkflowToRun(workflowRunId, triggerType, triggerExecutorId, workflowId, request, workflow, context);
        } finally {
            // Clean up the logging context when workflow execution completes
            LogContextManager.cleanup(workflowRunId.toString());
        }
    }

    private void processWorkflowToRun(UUID workflowRunId,
                                      TriggerType triggerType,
                                      UUID triggerExecutorId,
                                      UUID workflowId,
                                      WorkflowRunnerRequest request,
                                      Workflow workflow,
                                      RuntimeContext context) {
        try {
            // This will create a new run if it doesn't exist or return the existing one.
            WorkflowRun workflowRun = workflowRunService.findOrCreateWorkflowRun(workflowRunId, workflowId, triggerType);

            // Extract the consumer map from the static context in the workflow definition
            WorkflowMetadata metadata = workflow.getDefinition().metadata();
            Map<String, Set<String>> consumers = metadata.nodeConsumers().entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> Set.of(entry.getValue().toString())
                    ));
            Map<String, String> aliasMap = metadata.aliases();

            String startFromNodeKey = getStartNodeKey(workflow.getDefinition().nodes(), request, triggerExecutorId);
            initializeContext(workflowRunId, workflowId, request, workflowRun, context, consumers, aliasMap, startFromNodeKey);

            WorkflowExecutionStatus status = workflowEngineService.runWorkflow(workflow, workflowRunId, startFromNodeKey, context);

            handleWorkflowExecutionStatus(workflowRunId, workflowId, status, context);

        } catch (Exception e) {
            log.warn("Error running workflow with ID: {}", workflowId, e);
            workflowRunService.handleWorkflowError(workflowRunId, e);

            Object callbackUrlObj = context.get(ExecutionContextKey.CALLBACK_URL.key());
            if (callbackUrlObj instanceof String callbackUrl && !callbackUrl.isEmpty()) {
                notifyCallbackUrl(callbackUrl, workflowRunId);
            }
        } finally {
            contextManager.remove(workflowRunId.toString());
        }
    }

    private void initializeContext(UUID workflowRunId,
                                   UUID workflowId,
                                   WorkflowRunnerRequest request,
                                   WorkflowRun workflowRun,
                                   RuntimeContext context,
                                   Map<String, Set<String>> consumers,
                                   Map<String, String> aliasMap,
                                   String startNodeKey) {
        if (workflowRun.getContext() == null || workflowRun.getContext().isEmpty()) {
            // First run: ensure the run is started and create a new context
            log.debug("No existing context found for workflow run ID: {}. Starting new run.", workflowRunId);
            AggregatedSecretSetupDto agg = secretAggregateService.getAggregatedSecretsProfilesAndNodeIndex(workflowId);

            // Build per-node profile view expected by ExecutionContext.getProfileSecret
            Map<String, Object> profilesByNodeKey = agg.nodeProfiles().entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> {
                                String profileId = e.getValue();
                                Map<String, String> secrets = agg.profiles().getOrDefault(profileId, Map.of());
                                return new ConcurrentHashMap<>(Map.of(
                                        "secrets", secrets,
                                        "profileId", profileId,
                                        "profileName", agg.profileNames().getOrDefault(profileId, null)
                                ));
                            }
                    ));

            // Build per-node secrets view expected by ExecutionContext.getSecret
            Map<String, Object> secretsByNodeKey = agg.nodeSecrets().entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> {
                                Map<String, String> nodeSecretMap = e.getValue().stream()
                                        .collect(Collectors.toMap(
                                                sid -> agg.secretKeys().getOrDefault(sid, sid),
                                                sid -> agg.secrets().get(sid),
                                                (a, b) -> b
                                        ));
                                return new ConcurrentHashMap<>(nodeSecretMap);
                            }
                    ));

            Map<String, Object> initialContext = new ConcurrentHashMap<>();
            initialContext.put(ExecutionContextKey.SECRET_KEY.key(), secretsByNodeKey);
            initialContext.put(ExecutionContextKey.PROFILE_KEY.key(), profilesByNodeKey);

            if (request != null && request.callbackUrl() != null && !request.callbackUrl().isEmpty()) {
                initialContext.put(ExecutionContextKey.CALLBACK_URL.key(), request.callbackUrl());
            }

            if (request != null && request.payload() != null && startNodeKey != null) {
                Map<String, Object> payload = ObjectConversion.convertObjectToMap(request.payload());
                Map<String, Object> flattenedPayload = MapUtils.flattenMap(payload);
                for (Map.Entry<String, Object> entry : flattenedPayload.entrySet()) {
                    initialContext.put(String.format("%s.output.payload.%s", startNodeKey, entry.getKey()), entry.getValue());
                }
            }

            context.initialize(initialContext, consumers, aliasMap);
        } else {
            // Resumed run: load existing context
            log.debug("Existing context found for workflow run ID: {}. Loading context.", workflowRunId);
            context.initialize(new ConcurrentHashMap<>(workflowRun.getContext()), consumers, aliasMap);
        }
    }

    private void handleWorkflowExecutionStatus(UUID workflowRunId,
                                               UUID workflowId,
                                               WorkflowExecutionStatus status,
                                               RuntimeContext context) {
        if (status == WorkflowExecutionStatus.COMPLETED) {
            workflowRunService.completeWorkflowRun(workflowRunId);
            log.debug("Workflow with ID: {} completed successfully", workflowId);

            Object callbackUrlObj = context.get(ExecutionContextKey.CALLBACK_URL.key());
            if (callbackUrlObj instanceof String callbackUrl && !callbackUrl.isEmpty()) {
                notifyCallbackUrl(callbackUrl, workflowRunId);
            }
        } else if (status == WorkflowExecutionStatus.HALTED) {
            // Workflow is paused (RETRY or WAITING), save the context for resumption.
            log.debug("Workflow with ID: {} is halted. Saving context.", workflowId);
            workflowRunService.saveContext(workflowRunId, context.getContext());
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

    private String getStartNodeKey(WorkflowNodes nodes, WorkflowRunnerRequest request, UUID triggerExecutorId) {
        if (triggerExecutorId != null) {
            return nodes.findByNodeId(triggerExecutorId).getKey();
        }

        if (request == null) {
            return null;
        }

        if (request.startFromNodeKey() != null && nodes.findByInstanceKey(request.startFromNodeKey()) == null) {
                throw new IllegalArgumentException("Invalid startFromNodeKey: " + request.startFromNodeKey());
        } else {
            return request.startFromNodeKey();
        }
    }
}
