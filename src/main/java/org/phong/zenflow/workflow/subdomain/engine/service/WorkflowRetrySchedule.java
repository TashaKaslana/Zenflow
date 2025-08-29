package org.phong.zenflow.workflow.subdomain.engine.service;

import lombok.AllArgsConstructor;
import org.phong.zenflow.core.services.SharedQuartzSchedulerService;
import org.phong.zenflow.workflow.subdomain.engine.exception.WorkflowEngineException;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
@AllArgsConstructor
public class WorkflowRetrySchedule {
    public static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MILLIS = 5000; // Default retry delay in milliseconds
    private final SharedQuartzSchedulerService scheduler;

    public void scheduleRetry(UUID workflowId, UUID workflowRunId, String nodeKey, Integer attempts) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("workflowId", workflowId.toString());
        jobDataMap.put("workflowRunId", workflowRunId.toString());
        jobDataMap.put("nodeKey", nodeKey);

        String jobId = workflowRunId + ":" + nodeKey;
        String triggerId = "trigger-" + jobId;
        String jobGroup = "retry-jobs";
        String triggerGroup = "retry-triggers";

        JobDetail jobDetail = JobBuilder.newJob(WorkflowRetryJob.class)
                .withIdentity(jobId, jobGroup)
                .setJobData(jobDataMap)
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerId, triggerGroup)
                .startAt(Date.from(Instant.now().plusMillis(calculateRetryDelay(attempts))))
                .build();

        try {
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (Exception e) {
            throw new WorkflowEngineException("Failed to schedule workflow retry job", e);
        }
    }

    //2^0 -> 1, 2^1 -> 2, 2^2 -> 4, 2^3 -> 8, ...
    private long calculateRetryDelay(int attempts) {
        return RETRY_DELAY_MILLIS * (long) Math.pow(2, attempts - 1);
    }
}
