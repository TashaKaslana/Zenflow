package org.phong.zenflow.workflow.subdomain.context;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global pool of {@link RuntimeContext} instances keyed by workflow run id.
 */
public final class RuntimeContextPool {
    private static final Map<UUID, RuntimeContext> CONTEXTS = new ConcurrentHashMap<>();

    private RuntimeContextPool() {
    }

    public static void registerContext(UUID workflowRunId, RuntimeContext context) {
        CONTEXTS.put(workflowRunId, context);
    }

    public static RuntimeContext getContext(UUID workflowRunId) {
        return CONTEXTS.get(workflowRunId);
    }

    public static void removeContext(UUID workflowRunId) {
        CONTEXTS.remove(workflowRunId);
    }

    public static void clear() {
        CONTEXTS.clear();
    }
}
