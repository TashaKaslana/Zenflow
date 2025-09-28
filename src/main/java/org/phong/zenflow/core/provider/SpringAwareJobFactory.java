package org.phong.zenflow.core.provider;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;
import org.springframework.stereotype.Component;

/**
 * Spring-aware job factory for Quartz that enables dependency injection in job classes.
 * This follows the same pattern as your Spring component management - ensuring proper DI.
 */
@Component
@AllArgsConstructor
public class SpringAwareJobFactory extends SpringBeanJobFactory {

    private final AutowireCapableBeanFactory beanFactory;

    @NotNull
    @Override
    protected Object createJobInstance(@NotNull TriggerFiredBundle bundle) throws Exception {
        Object jobInstance = super.createJobInstance(bundle);
        // Enable Spring dependency injection for the job instance
        beanFactory.autowireBean(jobInstance);
        return jobInstance;
    }
}
