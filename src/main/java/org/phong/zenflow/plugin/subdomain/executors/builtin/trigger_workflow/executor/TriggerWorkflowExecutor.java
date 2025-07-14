package org.phong.zenflow.plugin.subdomain.executors.builtin.trigger_workflow.executor;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.workflow.subdomain.runner.dto.WorkflowRunnerRequest;
import org.phong.zenflow.workflow.subdomain.runner.event.WorkflowRunnerPublishableEvent;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Component
@AllArgsConstructor
public class TriggerWorkflowExecutor implements PluginNodeExecutor {
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public String key() {
        return "core.trigger_workflow";
    }

    @Override
    @Transactional
    public ExecutionResult execute(Map<String, Object> config, Map<String, Object> context) {
        //TODO: ensure trigger workflow is enabled, exists and is own by the user
        UUID workflowRunId = (UUID) config.get("workflow_run_id");
        UUID workflowId = (UUID) config.get("workflow_id");
//        boolean isPassContext = (boolean) config.getOrDefault("is_pass_context", false);
        boolean isAsync = (boolean) config.getOrDefault("is_async", false);

        eventPublisher.publishEvent(new WorkflowRunnerPublishableEvent() {
            @Override
            public UUID getWorkflowRunId() {
                return workflowRunId;
            }

            @Override
            public TriggerType getTriggerType() {
                return isAsync ? TriggerType.EVENT : TriggerType.MANUAL;
            }

            @Override
            public UUID getWorkflowId() {
                return workflowId;
            }

            @Override
            public WorkflowRunnerRequest request() {
                return null;
            }
        });
        return null;
    }
}
