package org.phong.zenflow.setup;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ContextSetupHolder {
    private static final Map<String, Object> context = new ConcurrentHashMap<>();

    public static void set(String key, Object value) {
        context.put(key, value);
    }

    public static Object get(String key) {
        return context.get(key);
    }

    public static void clear() {
        context.clear();
    }

    public static Map<String, Object> getAll() {
        return new HashMap<>(context);
    }
}
