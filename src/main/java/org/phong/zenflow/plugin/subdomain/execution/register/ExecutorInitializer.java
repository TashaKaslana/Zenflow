package org.phong.zenflow.plugin.subdomain.execution.register;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.plugin.subdomain.execution.registry.PluginNodeExecutorRegistry;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExecutorInitializer implements ApplicationListener<ContextRefreshedEvent> {

    private final ApplicationContext applicationContext;
    private final PluginNodeExecutorRegistry registry;

    @Override
    public void onApplicationEvent(@NonNull ContextRefreshedEvent event) {
        String[] beanNames = applicationContext.getBeanNamesForType(PluginNodeExecutor.class);
        for (String beanName : beanNames) {
            PluginNodeExecutor executor = applicationContext.getBean(beanName, PluginNodeExecutor.class);
            PluginNodeIdentifier identifier = PluginNodeIdentifier.fromString(executor.key());
            registry.register(identifier, () -> applicationContext.getBean(beanName, PluginNodeExecutor.class));
        }
    }
}
