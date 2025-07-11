package org.phong.zenflow.plugin.subdomain.executor.register;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.plugin.subdomain.executor.interfaces.PluginNodeExecutor;
import org.phong.zenflow.plugin.subdomain.executor.registry.PluginNodeExecutorRegistry;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ExecutorInitializer implements ApplicationListener<ContextRefreshedEvent> {

    private final List<PluginNodeExecutor> allExecutors;
    private final PluginNodeExecutorRegistry registry;

    @Override
    @NonNull
    public void onApplicationEvent(ContextRefreshedEvent event) {
        allExecutors.forEach(registry::register);
    }
}
