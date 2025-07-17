package org.phong.zenflow.workflow.subdomain.engine.service;

import lombok.AllArgsConstructor;
import org.phong.zenflow.workflow.subdomain.runner.dto.WorkflowRunnerRequest;
import org.phong.zenflow.workflow.subdomain.runner.event.WorkflowRunnerPublishableEvent;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

@AllArgsConstructor
public class WorkflowRetryJob implements Job {
    private final ApplicationEventPublisher publisher;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        publisher.publishEvent(new WorkflowRunnerPublishableEvent() {
            @Override
            public UUID getWorkflowRunId() {
                return UUID.fromString(dataMap.getString("workflowRunId"));
            }

            @Override
            public TriggerType getTriggerType() {
                return TriggerType.SCHEDULE;
            }

            @Override
            public UUID getWorkflowId() {
                return UUID.fromString(dataMap.getString("workflowId"));
            }

            @Override
            public WorkflowRunnerRequest request() {
                return new WorkflowRunnerRequest(
                        null,
                        dataMap.getString("nodeKey")
                );
            }
        });
    }
}
