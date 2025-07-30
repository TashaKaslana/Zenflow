package org.phong.zenflow.workflow.subdomain.engine.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.engine.dto.WorkflowExecutionStatus;
import org.phong.zenflow.workflow.subdomain.engine.exception.WorkflowEngineException;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class WorkflowNavigatorService {
    public ExecutionStepOutcome handleExecutionResult(UUID workflowId,
                                                      BaseWorkflowNode workingNode,
                                                      ExecutionResult result,
                                                      List<BaseWorkflowNode> workflowNodes,
                                                      RuntimeContext context) {
        return switch (result.getStatus()) {
            case SUCCESS -> new ExecutionStepOutcome(navigatorSuccess(workflowId, workingNode, workflowNodes),
                    WorkflowExecutionStatus.COMPLETED);
            case ERROR -> {
                context.endLoopIfActive();
                log.error("Workflow in node completed with error: {}", result.getError());
                throw new WorkflowEngineException("Workflow execution failed at node: " + workingNode.getKey());
            }
            case RETRY, WAITING -> {
                log.info("Workflow is now in {} state at node {}. Halting execution.", result.getStatus(), workingNode.getKey());
                yield new ExecutionStepOutcome(null, WorkflowExecutionStatus.HALTED);
            }
            case NEXT -> new ExecutionStepOutcome(navigatorNext(workflowId, result, workflowNodes),
                    WorkflowExecutionStatus.COMPLETED);
            case VALIDATION_ERROR -> new ExecutionStepOutcome(null, navigatorValidationError(workingNode, result, context));
            case LOOP_NEXT -> new ExecutionStepOutcome(navigatorLoopNext(workflowId, workingNode, result, workflowNodes, context),
                    WorkflowExecutionStatus.COMPLETED);
            case LOOP_END -> new ExecutionStepOutcome(navigatorLoopEnd(workflowId, workingNode, result, workflowNodes, context),
                    WorkflowExecutionStatus.COMPLETED);
            case LOOP_CONTINUE -> {
                navigatorLoopContinue(workingNode);
                yield new ExecutionStepOutcome(workingNode, WorkflowExecutionStatus.COMPLETED);
            }
            case LOOP_BREAK -> new ExecutionStepOutcome(navigatorLoopBreak(workflowId, workingNode, result, workflowNodes, context),
                    WorkflowExecutionStatus.COMPLETED);
        };
    }

    public BaseWorkflowNode findNodeByKey(List<BaseWorkflowNode> schema, String key) {
        return schema.stream()
                .filter(node -> node.getKey().equals(key))
                .findFirst()
                .orElse(null);
    }

    private BaseWorkflowNode navigatorSuccess(UUID workflowId,
                                              BaseWorkflowNode workingNode,
                                              List<BaseWorkflowNode> workflowSchema) {
        String nextNodeKey = workingNode.getNext().isEmpty() ? null : workingNode.getNext().getFirst();
        if (nextNodeKey == null) {
            log.info("Workflow completed successfully with ID: {}", workflowId);
            workingNode = null; // End of workflow
        } else {
            workingNode = findNodeByKey(workflowSchema, nextNodeKey);
        }
        return workingNode;
    }

    private BaseWorkflowNode navigatorNext(UUID workflowId,
                                           ExecutionResult result,
                                           List<BaseWorkflowNode> workflowSchema) {
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

    private WorkflowExecutionStatus navigatorValidationError(BaseWorkflowNode workingNode, ExecutionResult result, RuntimeContext context) {
        log.warn("Validation error in node {}: {}", workingNode.getKey(), result.getValidationResult());
        context.endLoopIfActive();
        return WorkflowExecutionStatus.HALTED;
    }

    private BaseWorkflowNode navigatorLoopNext(UUID workflowId,
                                               BaseWorkflowNode workingNode,
                                               ExecutionResult result,
                                               List<BaseWorkflowNode> workflowNodes,
                                               RuntimeContext context) {
        // Start loop if not already started (first iteration)
        if (!context.isInLoop()) {
            context.startLoop(workingNode.getKey());
            log.debug("Started loop for node: {}", workingNode.getKey());
        }

        String nextNodeKey = result.getNextNodeKey();
        if (nextNodeKey != null) {
            log.debug("Loop proceeding to next iteration at node: {}", nextNodeKey);
            return findNodeByKey(workflowNodes, nextNodeKey);
        } else {
            log.info("Loop next navigation has no target node, workflow completed with ID: {}", workflowId);
            return null;
        }
    }

    private BaseWorkflowNode navigatorLoopEnd(UUID workflowId,
                                              BaseWorkflowNode workingNode,
                                              ExecutionResult result,
                                              List<BaseWorkflowNode> workflowNodes,
                                              RuntimeContext context) {
        // End the loop
        context.endLoop(workingNode.getKey());
        log.debug("Loop ended for node: {}", workingNode.getKey());

        String nextNodeKey = result.getNextNodeKey();
        if (nextNodeKey != null) {
            log.debug("Loop ended, proceeding to node: {}", nextNodeKey);
            return findNodeByKey(workflowNodes, nextNodeKey);
        } else {
            log.info("Loop ended with no next node, workflow completed with ID: {}", workflowId);
            return null;
        }
    }

    private void navigatorLoopContinue(BaseWorkflowNode currentNode) {
        // For LOOP_CONTINUE, we don't start/end the loop, just stay at current node
        log.debug("Loop continue - staying at current node: {}", currentNode.getKey());
    }

    private BaseWorkflowNode navigatorLoopBreak(UUID workflowId,
                                                BaseWorkflowNode workingNode,
                                                ExecutionResult result,
                                                List<BaseWorkflowNode> workflowNodes,
                                                RuntimeContext context) {
        // End the loop when breaking
        context.endLoop(workingNode.getKey());
        log.debug("Loop break - ended loop for node: {}", workingNode.getKey());

        String nextNodeKey = result.getNextNodeKey();
        if (nextNodeKey != null) {
            log.debug("Loop break - proceeding to node: {}", nextNodeKey);
            return findNodeByKey(workflowNodes, nextNodeKey);
        } else {
            log.info("Loop break with no next node, workflow completed with ID: {}", workflowId);
            return null;
        }
    }

    public record ExecutionStepOutcome(BaseWorkflowNode nextNode, WorkflowExecutionStatus status) {
    }
}
