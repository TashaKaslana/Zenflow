package org.phong.zenflow.workflow.subdomain.context;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class RuntimeContextFactory {
    
    /**
     * Create a new RuntimeContext instance for each workflow run
     * Each workflow run gets its own SystemStateManager instance
     */
    public RuntimeContext createRuntimeContext() {
        SystemStateManager systemStateManager = new SystemStateManager();
        RuntimeContext runtimeContext = new RuntimeContext(systemStateManager);
        log.debug("Created new RuntimeContext with SystemStateManager for workflow run");
        return runtimeContext;
    }

    /**
     * Create RuntimeContext with initial data for workflow run
     */
    public RuntimeContext createRuntimeContext(Map<String, Object> initialContext,
                                               Map<String, Set<String>> initialConsumers,
                                               Map<String, String> initialAliases) {
        RuntimeContext runtimeContext = createRuntimeContext();
        runtimeContext.initialize(initialContext, initialConsumers, initialAliases);
        return runtimeContext;
    }
}
