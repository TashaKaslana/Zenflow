package org.phong.zenflow.workflow.subdomain.engine.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.services.PluginNodeExecutorDispatcher;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.repository.PluginNodeRepository;
import org.phong.zenflow.secret.service.SecretService;
import org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow;
import org.phong.zenflow.workflow.infrastructure.persistence.repository.WorkflowRepository;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@AllArgsConstructor
@Slf4j
public class WorkflowEngineService {
    private final WorkflowRepository workflowRepository;
    private final ObjectMapper objectMapper;
    private final PluginNodeExecutorDispatcher executorDispatcher;
    private final PluginNodeRepository pluginNodeRepository;
    private final SecretService secretService;
    private final NodeExecutorRegistry nodeExecutorRegistry;
    private final NodeLogService nodeLogService;
    private final WorkflowRetrySchedule workflowRetrySchedule;

    public boolean runWorkflow(UUID workflowId, UUID workflowRunId, @Nullable String startFromNodeKey) {
        Map<String, Object> secretOfWorkflow = ObjectConversion.convertObjectToMap(secretService.getSecretsByWorkflowId(workflowId));
        Map<String, Object> context = new ConcurrentHashMap<>(Map.of("secrets", secretOfWorkflow));

        try {
            Workflow workflow = workflowRepository.findById(workflowId).orElseThrow(
                    () -> new WorkflowEngineException("Workflow not found with ID: " + workflowId)
            );

            Map<String, Object> definition = workflow.getDefinition();
            List<BaseWorkflowNode> workflowSchema = objectMapper.readValue((JsonParser) definition.get("nodes"), new TypeReference<>() {
            });

            //currentNodeKey is a start node for a new workflow run or a node to resume, retry from
            String currentNodeKey = (startFromNodeKey != null) ? startFromNodeKey : workflow.getStartNode();
            BaseWorkflowNode workingNode = findNodeByKey(workflowSchema, currentNodeKey);
            while (workingNode != null) {
                if (workingNode.getType() == NodeType.PLUGIN) {
                    workingNode = getNextNodeAfterPluginNode(workflowId, workflowRunId, workingNode, currentNodeKey, context, workflowSchema);
                } else {
                    ExecutionResult result = nodeExecutorRegistry.execute(workingNode, context);
                    context.put(workingNode.getKey(), result.getOutput());

                    String nextNodeKey = result.getNextNodeKey();
                    if (nextNodeKey == null) {
                        log.info("Workflow completed successfully with ID: {}", workflowId);
                        break; // Exit loop if no next node is defined
                    }
                    workingNode = workflowSchema.stream().filter(node -> node.getKey().equals(nextNodeKey)).findFirst().orElse(null);
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("Error running workflow with ID: {}", workflowId, e);
            return false;
        }
    }

    private BaseWorkflowNode getNextNodeAfterPluginNode(UUID workflowId, UUID workflowRunId, BaseWorkflowNode workingNode, String startNodeKey,
                                                        Map<String, Object> context, List<BaseWorkflowNode> workflowSchema) {
        PluginDefinition pluginDefinition = (PluginDefinition) workingNode;
        PluginNode pluginNode = pluginNodeRepository.findById(pluginDefinition.getPluginNode().pluginId()).orElseThrow(
                () -> new WorkflowEngineException("Plugin node not found with ID: " + startNodeKey)
        );
        nodeLogService.startNode(workflowRunId, workingNode.getKey());
        ExecutionResult result = executorDispatcher.dispatch(pluginNode, workingNode.getConfig(), context);
        resolveNodeLog(workflowId, workflowRunId, workingNode, result);

        context.put(workingNode.getKey(), result.getOutput());

        String nextNodeKey = workingNode.getNext().getFirst(); // Assuming single next node for simplicity
        workingNode = workflowSchema.stream().filter(node -> node.getKey().equals(nextNodeKey)).findFirst().orElse(null);
        return workingNode;
    }

    private BaseWorkflowNode findNodeByKey(List<BaseWorkflowNode> schema, String key) {
        return schema.stream()
                .filter(node -> node.getKey().equals(key))
                .findFirst()
                .orElse(null);
    }

    private void resolveNodeLog(UUID workflowId, UUID workflowRunId, BaseWorkflowNode workingNode, ExecutionResult result) {
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
            default:
                log.warn("Unknown status for plugin node execution: {}", result.getStatus());
        }
    }
}
