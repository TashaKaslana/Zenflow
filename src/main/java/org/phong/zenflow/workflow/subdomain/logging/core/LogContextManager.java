package org.phong.zenflow.workflow.subdomain.logging.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

public class LogContextManager {
    private static final ThreadLocal<Deque<String>> stack = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<String> traceId = new ThreadLocal<>();

    public static void init(String trace) {
        traceId.set(trace);
        stack.get().clear();
    }

    public static void push(String component) {
        stack.get().push(component);
    }

    public static void pop() {
        Deque<String> deque = stack.get();
        if (!deque.isEmpty()) {
            deque.pop();
        }
    }

    public static LogContext snapshot() {
        return new LogContext(traceId.get(), String.join("->", stack.get()));
    }

    public static <T> T withComponent(String name, Supplier<T> action) {
        push(name);
        try {
            return action.get();
        } finally {
            pop();
        }
    }
}

