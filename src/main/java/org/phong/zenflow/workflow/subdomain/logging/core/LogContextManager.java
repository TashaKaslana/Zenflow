package org.phong.zenflow.workflow.subdomain.logging.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class LogContextManager {
    // Store multiple contexts per thread, keyed by context ID
    private static final ThreadLocal<Map<String, ContextData>> contexts =
        ThreadLocal.withInitial(HashMap::new);
    private static final ThreadLocal<String> activeContextId = new ThreadLocal<>();

    private record ContextData(String traceId, Deque<String> stack) {}

    /**
     * Initialize a new context with a unique context ID (typically workflow run ID)
     */
    public static void init(String contextId, String traceId) {
        Map<String, ContextData> threadContexts = contexts.get();
        threadContexts.put(contextId, new ContextData(traceId, new ArrayDeque<>()));
        activeContextId.set(contextId);
    }

    /**
     * Switch to an existing context
     */
    public static void switchTo(String contextId) {
        Map<String, ContextData> threadContexts = contexts.get();
        if (threadContexts.containsKey(contextId)) {
            activeContextId.set(contextId);
        } else {
            throw new IllegalStateException("Context not found: " + contextId);
        }
    }

    /**
     * Get the current active context ID
     */
    public static String getCurrentContextId() {
        return activeContextId.get();
    }

    /**
     * Execute code within a specific context
     */
    public static <T> T withContext(String contextId, Supplier<T> action) {
        String previousContext = activeContextId.get();
        try {
            switchTo(contextId);
            return action.get();
        } finally {
            if (previousContext != null) {
                activeContextId.set(previousContext);
            }
        }
    }

    public static void push(String component) {
        ContextData context = getCurrentContext();
        if (context != null) {
            context.stack().push(component);
        }
    }

    public static void pop() {
        ContextData context = getCurrentContext();
        if (context != null && !context.stack().isEmpty()) {
            context.stack().pop();
        }
    }

    public static LogContext snapshot() {
        ContextData context = getCurrentContext();
        if (context == null) {
            return new LogContext(null, "");
        }
        return new LogContext(
            context.traceId(),
            String.join("->", (Iterable<String>) () -> context.stack().descendingIterator())
        );
    }

    /**
     * Execute code with a component pushed to the current context's stack
     */
    public static <T> T withComponent(String name, Supplier<T> action) {
        push(name);
        try {
            return action.get();
        } finally {
            pop();
        }
    }

    /**
     * Clean up a specific context (call this when workflow completes)
     */
    public static void cleanup(String contextId) {
        Map<String, ContextData> threadContexts = contexts.get();
        threadContexts.remove(contextId);

        // If we're cleaning up the active context, clear it
        if (contextId.equals(activeContextId.get())) {
            activeContextId.remove();
        }
    }

    /**
     * Clean up all contexts for current thread
     */
    public static void cleanupAll() {
        contexts.remove();
        activeContextId.remove();
    }

    private static ContextData getCurrentContext() {
        String contextId = activeContextId.get();
        if (contextId == null) {
            return null;
        }
        return contexts.get().get(contextId);
    }
}
