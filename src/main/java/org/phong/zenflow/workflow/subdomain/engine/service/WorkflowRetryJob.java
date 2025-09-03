package org.phong.zenflow.workflow.subdomain.engine.service;

import lombok.AllArgsConstructor;
import org.phong.zenflow.workflow.subdomain.runner.dto.WorkflowRunnerRequest;
import org.phong.zenflow.workflow.subdomain.trigger.dto.WorkflowTriggerEvent;
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
        UUID workflowRunId = UUID.fromString(dataMap.getString("workflowRunId"));
        UUID workflowId = UUID.fromString(dataMap.getString("workflowId"));
        String nodeKey = dataMap.getString("nodeKey");
        String callBackUrl = dataMap.getString("callbackUrl");

        publisher.publishEvent(new WorkflowTriggerEvent(
                workflowRunId,
                TriggerType.SCHEDULE_RETRY,
                workflowId,
                new WorkflowRunnerRequest(callBackUrl, nodeKey)
        ));
    }
}
