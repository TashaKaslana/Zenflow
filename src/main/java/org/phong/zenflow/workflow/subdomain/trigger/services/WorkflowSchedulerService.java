package org.phong.zenflow.workflow.subdomain.trigger.services;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;
import org.phong.zenflow.workflow.subdomain.trigger.exception.WorkflowTriggerException;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class WorkflowSchedulerService {
    private final Scheduler scheduler;

    public void registerSchedule(WorkflowTrigger trigger) {
        JsonNode config = (JsonNode) trigger.getConfig();
        String cron = config.has("cron") ? config.get("cron").asText() : null;
        String timezone = config.has("timezone") ? config.get("timezone").asText() : "UTC";

        if (cron == null || !trigger.getEnabled()) return;

        JobDataMap dataMap = new JobDataMap();
        dataMap.put("workflowId", trigger.getWorkflowId().toString());

        JobDetail jobDetail = JobBuilder.newJob(ScheduledWorkflowJob.class)
                .withIdentity("job-" + trigger.getId())
                .usingJobData(dataMap)
                .storeDurably()
                .build();

        Trigger quartzTrigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger-" + trigger.getId())
                .forJob(jobDetail)
                .withSchedule(CronScheduleBuilder.cronSchedule(cron)
                        .inTimeZone(TimeZone.getTimeZone(timezone)))
                .build();

        try {
            if (scheduler.checkExists(jobDetail.getKey())) {
                scheduler.deleteJob(jobDetail.getKey());
            }
            scheduler.scheduleJob(jobDetail, quartzTrigger);
        } catch (SchedulerException e) {
            throw new WorkflowTriggerException("Failed to schedule job: " + trigger.getId(), e);
        }
    }

    public void registerAll(List<WorkflowTrigger> triggers) {
        for (WorkflowTrigger trigger : triggers) {
            if (trigger.getType() == TriggerType.SCHEDULE && trigger.getEnabled()) {
                registerSchedule(trigger);
            }
        }
    }

    public void removeSchedule(UUID triggerId) {
        try {
            scheduler.deleteJob(JobKey.jobKey("job-" + triggerId));
        } catch (SchedulerException e) {
            log.error("Failed to remove schedule for trigger ID: {}", triggerId, e);
            throw new WorkflowTriggerException("Failed to remove schedule for trigger ID: " + triggerId, e);
        }
    }
}
