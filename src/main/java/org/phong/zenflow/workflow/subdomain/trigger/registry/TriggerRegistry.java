package org.phong.zenflow.workflow.subdomain.trigger.registry;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.registry.PluginNodeExecutorRegistry;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerExecutor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Getter
@AllArgsConstructor
@Slf4j
@Component
public class TriggerRegistry {
    private final PluginNodeExecutorRegistry registry;
    private final Set<String> triggerExecutorIds = new HashSet<>();

    public void registerTrigger(String key) {
        triggerExecutorIds.add(key);
    }

    public void unregisterTrigger(String key) {
        triggerExecutorIds.remove(key);
    }

    public TriggerExecutor getRegistry(String triggerKey) {
        if (!triggerExecutorIds.contains(triggerKey) && registry.getExecutor(triggerKey).isEmpty()) {
            log.warn("Trigger not found for key {}", triggerKey);
            return null;
        }

        return (TriggerExecutor) registry.getExecutor(triggerKey).get();
    }
}
