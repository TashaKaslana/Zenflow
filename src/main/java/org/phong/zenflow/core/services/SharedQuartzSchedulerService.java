package org.phong.zenflow.core.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Service;

/**
 * Efficient scheduler service using a SINGLE shared Quartz scheduler.
 * This replaces the inefficient QuartzSchedulerResourceManager that created multiple schedulers.
 */
@Service
@AllArgsConstructor
@Slf4j
public class SharedQuartzSchedulerService {

    private final Scheduler scheduler; // Single shared instance from OptimizedQuartzConfig

    /**
     * Schedule a job using the shared scheduler instance.
     * Much more efficient than creating multiple scheduler instances.
     */
    public void scheduleJob(JobDetail job, Trigger trigger) {
        try {
            scheduler.scheduleJob(job, trigger);
            log.debug("Scheduled job {} with trigger {} on shared scheduler",
                     job.getKey(), trigger.getKey());
        } catch (SchedulerException e) {
            log.error("Failed to schedule job: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to schedule job", e);
        }
    }

    /**
     * Remove a job from the shared scheduler instance.
     */
    public void unscheduleJob(TriggerKey triggerKey) {
        try {
            scheduler.unscheduleJob(triggerKey);
            log.debug("Unscheduled job with trigger {} from shared scheduler", triggerKey);
        } catch (SchedulerException e) {
            log.error("Failed to unschedule job: {}", e.getMessage(), e);
        }
    }

    /**
     * Check if the shared scheduler is healthy and running.
     */
    public boolean isSchedulerHealthy() {
        try {
            return scheduler.isStarted() && !scheduler.isShutdown();
        } catch (SchedulerException e) {
            log.error("Error checking scheduler health: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get scheduler statistics for monitoring.
     */
    public SchedulerMetaData getSchedulerStats() {
        try {
            return scheduler.getMetaData();
        } catch (SchedulerException e) {
            log.error("Error getting scheduler stats: {}", e.getMessage(), e);
            return null;
        }
    }
}
