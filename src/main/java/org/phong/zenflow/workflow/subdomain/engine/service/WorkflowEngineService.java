package org.phong.zenflow.workflow.subdomain.engine.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.services.PluginNodeExecutorDispatcher;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode;
import org.phong.zenflow.plugin.subdomain.node.service.PluginNodeService;
import org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow;
import org.phong.zenflow.workflow.infrastructure.persistence.repository.WorkflowRepository;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.engine.dto.WorkflowExecutionStatus;
import org.phong.zenflow.workflow.subdomain.engine.exception.WorkflowEngineException;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.NodeExecutorRegistry;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.enums.NodeType;
import org.phong.zenflow.workflow.subdomain.node_logs.service.NodeLogService;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationResult;
import org.phong.zenflow.workflow.subdomain.schema_validator.service.WorkflowValidationService;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class WorkflowEngineService {
    private final WorkflowRepository workflowRepository;
    private final PluginNodeExecutorDispatcher executorDispatcher;
    private final PluginNodeService pluginNodeService;
    private final NodeExecutorRegistry nodeExecutorRegistry;
    private final NodeLogService nodeLogService;
    private final WorkflowValidationService workflowValidationService;



    @Transactional
    public WorkflowExecutionStatus runWorkflow(UUID workflowId, UUID workflowRunId, @Nullable String startFromNodeKey, RuntimeContext context) {
        try {
            Workflow workflow = workflowRepository.findById(workflowId).orElseThrow(
                    () -> new WorkflowEngineException("Workflow not found with ID: " + workflowId)
            );

            WorkflowDefinition definition = workflow.getDefinition();
            if (definition == null || definition.nodes() == null) {
                throw new WorkflowEngineException("Workflow definition or nodes are missing for workflow ID: " + workflowId);
            }
            List<BaseWorkflowNode> workflowSchema = definition.nodes();
            Map<UUID, PluginNode> pluginNodes = pluginNodeService.findAllByPluginId(
                    workflowSchema.stream()
                            .filter(node -> node.getType() == NodeType.PLUGIN)
                            .map(node -> ((PluginDefinition) node).getPluginNode().nodeId())
                            .toList()
            );

            WorkflowExecutionStatus executionStatus = WorkflowExecutionStatus.COMPLETED;

            String currentNodeKey = (startFromNodeKey != null) ? startFromNodeKey : workflow.getStartNode();
            BaseWorkflowNode workingNode = findNodeByKey(workflowSchema, currentNodeKey);

            while (workingNode != null) {
                ExecutionResult result;
                result = setupAndExecutionWorkflow(workflowId, workflowRunId, pluginNodes, context, workingNode);

                switch (result.getStatus()) {
                    case SUCCESS:
                        workingNode = navigatorSuccess(workflowId, workingNode, workflowSchema);
                        break;
                    case ERROR:
                        log.error("Workflow in node completed with error: {}", result.getError());
                        throw new WorkflowEngineException("Workflow execution failed at node: " + workingNode.getKey());
                    case RETRY:
                    case WAITING:
                        log.info("Workflow is now in {} state at node {}. Halting execution.", result.getStatus(), workingNode.getKey());
                        executionStatus = WorkflowExecutionStatus.HALTED;
                        workingNode = null;
                        break;
                    case NEXT:
                        workingNode = navigatorNext(workflowId, result, workflowSchema);
                        break;
                    case VALIDATION_ERROR:
                        executionStatus = navigatorValidationError(workingNode, result);
                        workingNode = null;
                        break;
                    default:
                        throw new WorkflowEngineException("Unknown execution status: " + result.getStatus());
                }
            }
            return executionStatus;
        } catch (Exception e) {
            log.warn("Error running workflow with ID: {}", workflowId, e);
            throw new WorkflowEngineException("Workflow failed", e);
        }
    }

    private ExecutionResult setupAndExecutionWorkflow(UUID workflowId, UUID workflowRunId, Map<UUID, PluginNode> pluginNodes, RuntimeContext context, BaseWorkflowNode workingNode) {
        ExecutionResult result;
        nodeLogService.startNode(workflowRunId, workingNode.getKey());

        WorkflowConfig config = workingNode.getConfig() != null ? workingNode.getConfig() : new WorkflowConfig();
        WorkflowConfig resolvedConfig = context.resolveConfig(workingNode.getKey(), config);

        result = executeWorkingNode(pluginNodes, context, workingNode, resolvedConfig);

        Map<String, Object> output = result.getOutput();
        if (output != null) {
            context.processOutputWithMetadata(String.format("%s.output", workingNode.getKey()), output);
        } else {
            log.warn("Output of node {} is null, skipping putting into context", workingNode.getKey());
        }
        nodeLogService.resolveNodeLog(workflowId, workflowRunId, workingNode, result);
        return result;
    }

    private ExecutionResult executeWorkingNode(Map<UUID, PluginNode> pluginNodes, RuntimeContext context, BaseWorkflowNode workingNode, WorkflowConfig resolvedConfig) {
        ExecutionResult result;
        if (workingNode.getType() == NodeType.PLUGIN) {
            UUID nodeId = ((PluginDefinition) workingNode).getPluginNode().nodeId();
            PluginNode pluginNode = pluginNodes.get(nodeId);
            if (pluginNode == null) {
                log.error("Plugin node with ID {} not found in workflow schema", nodeId);
                throw new WorkflowEngineException("Plugin node not found: " + nodeId);
            }

            ValidationResult validationResult = workflowValidationService.validateRuntime(
                    workingNode.getKey(),
                    resolvedConfig,
                    nodeId.toString()
            );
            if (!validationResult.isValid()) {
                return ExecutionResult.validationError(validationResult, workingNode.getKey());
            }

            result = executorDispatcher.dispatch(pluginNode, resolvedConfig);
        } else {
            result = nodeExecutorRegistry.execute(workingNode, context.getContext());
        }
        return result;
    }

    private BaseWorkflowNode navigatorSuccess(UUID workflowId, BaseWorkflowNode workingNode, List<BaseWorkflowNode> workflowSchema) {
        String nextNodeKey = workingNode.getNext().isEmpty() ? null : workingNode.getNext().getFirst();
        if (nextNodeKey == null) {
            log.info("Workflow completed successfully with ID: {}", workflowId);
            workingNode = null; // End of workflow
        } else {
            workingNode = findNodeByKey(workflowSchema, nextNodeKey);
        }
        return workingNode;
    }

    private BaseWorkflowNode navigatorNext(UUID workflowId, ExecutionResult result, List<BaseWorkflowNode> workflowSchema) {
        BaseWorkflowNode workingNode;
        String nextNode = result.getNextNodeKey();
        if (nextNode != null) {
            workingNode = findNodeByKey(workflowSchema, nextNode);
        } else {
            log.info("Reach the end of workflow, workflow completed successfully with ID: {}", workflowId);
            workingNode = null; // End of workflow
        }
        return workingNode;
    }

    private WorkflowExecutionStatus navigatorValidationError(BaseWorkflowNode workingNode, ExecutionResult result) {
        log.warn("Validation error in node {}: {}", workingNode.getKey(), result.getValidationResult());
        return WorkflowExecutionStatus.HALTED;
    }

    private BaseWorkflowNode findNodeByKey(List<BaseWorkflowNode> schema, String key) {
        return schema.stream()
                .filter(node -> node.getKey().equals(key))
                .findFirst()
                .orElse(null);
    }
}
