package org.phong.zenflow.workflow.subdomain.engine.service;

import lombok.AllArgsConstructor;
import org.phong.zenflow.workflow.subdomain.engine.event.WorkflowEngineEvent;
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
        publisher.publishEvent(new WorkflowEngineEvent(
                UUID.fromString(dataMap.getString("workflowId")),
                UUID.fromString(dataMap.getString("workflowRunId")),
                dataMap.getString("fromNodeKey")
        ));
    }
}
