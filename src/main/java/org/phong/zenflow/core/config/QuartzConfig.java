package org.phong.zenflow.core.config;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.provider.SpringAwareJobFactory;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

/**
 * Optimized Quartz configuration using Spring Boot auto-configuration.
 * This fixes the DataSource configuration issue by using Spring's approach.
 */
@Configuration
@AllArgsConstructor
@Slf4j
public class QuartzConfig {

    private final SpringAwareJobFactory springJobFactory;

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();

        // Enable Spring dependency injection
        factory.setJobFactory(springJobFactory);

        factory.setApplicationContextSchedulerContextKey("applicationContext");

        // Start scheduler automatically
        factory.setAutoStartup(true);

        return factory;
    }

    @Bean
    public Scheduler scheduler(SchedulerFactoryBean factory) throws SchedulerException {
        Scheduler scheduler = factory.getScheduler();
        log.info("Created optimized shared Quartz scheduler: {}", scheduler.getSchedulerName());
        return scheduler;
    }
}
