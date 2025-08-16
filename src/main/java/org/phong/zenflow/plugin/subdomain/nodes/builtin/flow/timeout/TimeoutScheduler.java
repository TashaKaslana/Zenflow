package org.phong.zenflow.plugin.subdomain.nodes.builtin.flow.timeout;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.plugin.subdomain.execution.exceptions.ExecutorException;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TimeoutScheduler {

    private final Scheduler scheduler;

    public void scheduleTimeout(UUID workflowId, UUID workflowRunId, String nodeKey, long delayMillis) {
        JobDataMap jobData = new JobDataMap();
        jobData.put("workflowId", workflowId.toString());
        jobData.put("workflowRunId", workflowRunId.toString());
        jobData.put("nodeKey", nodeKey);

        String jobId = workflowRunId + ":" + nodeKey;

        JobDetail job = JobBuilder.newJob(TimeoutResumeJob.class)
                .withIdentity(jobId, "timeout-jobs")
                .setJobData(jobData)
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger-" + jobId, "timeout-triggers")
                .startAt(Date.from(Instant.now().plusMillis(delayMillis)))
                .build();

        try {
            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            throw new ExecutorException("Failed to schedule timeout job", e);
        }
    }
}
