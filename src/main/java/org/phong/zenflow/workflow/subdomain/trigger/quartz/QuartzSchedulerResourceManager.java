package org.phong.zenflow.workflow.subdomain.trigger.quartz;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.workflow.subdomain.trigger.resource.BaseTriggerResourceManager;
import org.phong.zenflow.workflow.subdomain.trigger.resource.TriggerResourceConfig;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.stereotype.Component;

/**
 * Quartz Scheduler Resource Manager - handles sharing Quartz schedulers and job management.
 * Follows the exact same pattern as GlobalDbConnectionPool for consistent resource management.
 */
@Component
@AllArgsConstructor
@Slf4j
public class QuartzSchedulerResourceManager extends BaseTriggerResourceManager<Scheduler> {

    private final SchedulerFactory schedulerFactory = new StdSchedulerFactory();

    @Override
    protected Scheduler createResource(String resourceKey, TriggerResourceConfig config) {
        try {
            log.info("Creating new Quartz Scheduler for resource key: {}", resourceKey);

            // Create a new scheduler instance (similar to creating HikariDataSource)
            Scheduler scheduler = schedulerFactory.getScheduler();

            if (!scheduler.isStarted()) {
                scheduler.start();
            }

            log.info("Quartz Scheduler created and started for key: {}", resourceKey);
            return scheduler;

        } catch (SchedulerException e) {
            log.error("Failed to create Quartz Scheduler: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create Quartz Scheduler", e);
        }
    }

    @Override
    protected void cleanupResource(Scheduler scheduler) {
        try {
            log.info("Shutting down Quartz Scheduler");
            if (!scheduler.isShutdown()) {
                scheduler.shutdown(true); // Wait for jobs to complete
            }
        } catch (SchedulerException e) {
            log.error("Error shutting down Quartz Scheduler: {}", e.getMessage(), e);
        }
    }

    @Override
    protected boolean checkResourceHealth(Scheduler scheduler) {
        try {
            return scheduler.isStarted() && !scheduler.isShutdown();
        } catch (SchedulerException e) {
            log.error("Error checking scheduler health: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Schedule a job using the shared scheduler instance.
     * This is similar to how you use DataSource from the pool.
     */
    public void scheduleJob(String resourceKey, JobDetail job, Trigger trigger) {
        try {
            Scheduler scheduler = getExistingResource(resourceKey);
            if (scheduler != null) {
                scheduler.scheduleJob(job, trigger);
                log.debug("Scheduled job {} with trigger {} on scheduler: {}",
                         job.getKey(), trigger.getKey(), resourceKey);
            } else {
                log.error("No scheduler found for resource key: {}", resourceKey);
            }
        } catch (SchedulerException e) {
            log.error("Failed to schedule job on scheduler {}: {}", resourceKey, e.getMessage(), e);
            throw new RuntimeException("Failed to schedule job", e);
        }
    }

    /**
     * Remove a job from the shared scheduler instance.
     */
    public void unscheduleJob(String resourceKey, TriggerKey triggerKey) {
        try {
            Scheduler scheduler = getExistingResource(resourceKey);
            if (scheduler != null) {
                scheduler.unscheduleJob(triggerKey);
                log.debug("Unscheduled job with trigger {} from scheduler: {}",
                         triggerKey, resourceKey);
            }
        } catch (SchedulerException e) {
            log.error("Failed to unschedule job from scheduler {}: {}", resourceKey, e.getMessage(), e);
        }
    }
}
