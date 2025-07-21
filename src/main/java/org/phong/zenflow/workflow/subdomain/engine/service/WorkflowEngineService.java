package org.phong.zenflow.workflow.subdomain.engine.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.services.PluginNodeExecutorDispatcher;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.repository.PluginNodeRepository;
import org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow;
import org.phong.zenflow.workflow.infrastructure.persistence.repository.WorkflowRepository;
import org.phong.zenflow.workflow.subdomain.engine.dto.WorkflowExecutionStatus;
import org.phong.zenflow.workflow.subdomain.engine.exception.WorkflowEngineException;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.NodeExecutorRegistry;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.enums.NodeType;
import org.phong.zenflow.workflow.subdomain.node_logs.dto.NodeLogDto;
import org.phong.zenflow.workflow.subdomain.node_logs.enums.NodeLogStatus;
import org.phong.zenflow.workflow.subdomain.node_logs.service.NodeLogService;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class WorkflowEngineService {
    private final WorkflowRepository workflowRepository;
    private final ObjectMapper objectMapper;
    private final PluginNodeExecutorDispatcher executorDispatcher;
    private final PluginNodeRepository pluginNodeRepository;
    private final NodeExecutorRegistry nodeExecutorRegistry;
    private final NodeLogService nodeLogService;
    private final WorkflowRetrySchedule workflowRetrySchedule;

    @Transactional
    public WorkflowExecutionStatus runWorkflow(UUID workflowId, UUID workflowRunId, @Nullable String startFromNodeKey, Map<String, Object> context) {
        try {
            Workflow workflow = workflowRepository.findById(workflowId).orElseThrow(
                    () -> new WorkflowEngineException("Workflow not found with ID: " + workflowId)
            );

            Map<String, Object> definition = workflow.getDefinition();
            List<BaseWorkflowNode> workflowSchema = objectMapper.readValue(objectMapper.writeValueAsString(definition.get("nodes")), new TypeReference<>() {
            });

            String currentNodeKey = (startFromNodeKey != null) ? startFromNodeKey : workflow.getStartNode();
            BaseWorkflowNode workingNode = findNodeByKey(workflowSchema, currentNodeKey);

            while (workingNode != null) {
                ExecutionResult result;
                nodeLogService.startNode(workflowRunId, workingNode.getKey());

                if (workingNode.getType() == NodeType.PLUGIN) {
                    PluginDefinition pluginDefinition = (PluginDefinition) workingNode;
                    PluginNode pluginNode = pluginNodeRepository.findById(pluginDefinition.getPluginNode().nodeId()).orElseThrow(
                            () -> new WorkflowEngineException("Plugin node not found with ID: " + pluginDefinition.getPluginNode().pluginId())
                    );
                    result = executorDispatcher.dispatch(pluginNode, workingNode.getConfig());
                } else {
                    result = nodeExecutorRegistry.execute(workingNode, context);
                }

                Map<String, Object> output = result.getOutput();
                if (output != null) {
                    context.put(workingNode.getKey(), output);
                } else {
                    log.warn("Output of node {} is null, skipping putting into context", workingNode.getKey());
                }
                resolveNodeLog(workflowId, workflowRunId, workingNode, result);

                switch (result.getStatus()) {
                    case SUCCESS:
                        String nextNodeKey = workingNode.getNext().isEmpty() ? null : workingNode.getNext().getFirst();
                        if (nextNodeKey == null) {
                            log.info("Workflow completed successfully with ID: {}", workflowId);
                            workingNode = null; // End of workflow
                        } else {
                            workingNode = findNodeByKey(workflowSchema, nextNodeKey);
                        }
                        break;
                    case ERROR:
                        log.error("Workflow in node completed with error: {}", result.getError());
                        throw new WorkflowEngineException("Workflow execution failed at node: " + workingNode.getKey());
                    case RETRY:
                    case WAITING:
                        log.info("Workflow is now in {} state at node {}. Halting execution.", result.getStatus(), workingNode.getKey());
                        return WorkflowExecutionStatus.HALTED;
                    case NEXT:
                        String nextNode = result.getNextNodeKey();
                        if (nextNode != null) {
                            workingNode = findNodeByKey(workflowSchema, nextNode);
                        } else {
                            log.info("Reach the end of workflow, workflow completed successfully with ID: {}", workflowId);
                            workingNode = null; // End of workflow
                        }
                        break;
                    default:
                        throw new WorkflowEngineException("Unknown execution status: " + result.getStatus());
                }
            }
            return WorkflowExecutionStatus.COMPLETED;
        } catch (Exception e) {
            log.warn("Error running workflow with ID: {}", workflowId, e);
            throw new WorkflowEngineException("Workflow failed", e);
        }
    }

    private BaseWorkflowNode findNodeByKey(List<BaseWorkflowNode> schema, String key) {
        return schema.stream()
                .filter(node -> node.getKey().equals(key))
                .findFirst()
                .orElse(null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void resolveNodeLog(UUID workflowId, UUID workflowRunId, BaseWorkflowNode workingNode, ExecutionResult result) {
        switch (result.getStatus()) {
            case SUCCESS:
                log.debug("Plugin node executed successfully: {}", workingNode.getKey());
                nodeLogService.completeNode(workflowRunId, workingNode.getKey(), NodeLogStatus.SUCCESS, result.getError(), result.getOutput(), result.getLogs());
                break;
            case ERROR:
                log.error("Plugin node execution failed: {}", workingNode.getKey());
                nodeLogService.completeNode(workflowRunId, workingNode.getKey(), NodeLogStatus.ERROR, result.getError(), result.getOutput(), result.getLogs());
                break;
            case WAITING:
                log.debug("Plugin node execution skipped: {}", workingNode.getKey());
                nodeLogService.waitNode(workflowRunId, workingNode.getKey(), NodeLogStatus.WAITING, result.getLogs(), result.getError());
                break;
            case RETRY:
                log.debug("Plugin node execution retrying: {}", workingNode.getKey());
                NodeLogDto retryNode = nodeLogService.retryNode(workflowRunId, workingNode.getKey(), result.getLogs());
                workflowRetrySchedule.scheduleRetry(workflowId, workflowRunId, workingNode.getKey(), retryNode.attempts());
                break;
            case NEXT:
                log.debug("Plugin node execution next: {}", workingNode.getKey());
//                nodeLogService.nextNode(workflowRunId, workingNode.getKey(), NodeLogStatus.NEXT, result.getLogs(), result.getError());
                break;
            default:
                log.warn("Unknown status for plugin node execution: {}", result.getStatus());
        }
    }
}
