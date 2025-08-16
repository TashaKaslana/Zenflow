package org.phong.zenflow.plugin.subdomain.nodes.builtin.flow.timeout;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.workflow.subdomain.runner.dto.WorkflowRunnerRequest;
import org.phong.zenflow.workflow.subdomain.runner.event.WorkflowRunnerPublishableEvent;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@AllArgsConstructor
public class TimeoutResumeJob implements Job {
    private final ApplicationEventPublisher publisher;

    @Override
    public void execute(JobExecutionContext context) {
        JobDataMap dataMap = context.getMergedJobDataMap();
        UUID workflowId = UUID.fromString(dataMap.getString("workflowId"));
        UUID workflowRunId = UUID.fromString(dataMap.getString("workflowRunId"));
        String nodeKey = dataMap.getString("nodeKey");

        log.info("Resuming timeout node: {} in run {}", nodeKey, workflowRunId);
        publisher.publishEvent(new WorkflowRunnerPublishableEvent() {
            @Override
            public UUID getWorkflowRunId() {
                return workflowRunId;
            }

            @Override
            public TriggerType getTriggerType() {
                return TriggerType.SCHEDULE;
            }

            @Override
            public UUID getWorkflowId() {
                return workflowId;
            }

            @Override
            public WorkflowRunnerRequest request() {
                return new WorkflowRunnerRequest(null, nodeKey);
            }
        });
    }
}
