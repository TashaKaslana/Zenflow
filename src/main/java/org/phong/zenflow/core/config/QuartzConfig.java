package org.phong.zenflow.core.config;

import lombok.AllArgsConstructor;
import org.phong.zenflow.core.provider.SpringAwareJobFactory;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Quartz configuration that enables Spring dependency injection in job classes.
 * This follows the same configuration pattern as your database connection setup.
 */
@Configuration
@AllArgsConstructor
public class QuartzConfig {

    private final SpringAwareJobFactory springJobFactory;

    @Bean
    public SchedulerFactory schedulerFactory() {
        return new StdSchedulerFactory();
    }

    @Bean
    public Scheduler scheduler(SchedulerFactory schedulerFactory) throws Exception {
        Scheduler scheduler = schedulerFactory.getScheduler();

        // Set the Spring-aware job factory to enable dependency injection
        scheduler.setJobFactory(springJobFactory);

        return scheduler;
    }
}
