package org.phong.zenflow.workflow.subdomain.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility for navigating and mutating nested maps using dot-delimited paths.
 */
final class ContextPathAccessor {
    private final Map<String, Object> root;

    ContextPathAccessor(Map<String, Object> root) {
        this.root = Objects.requireNonNull(root, "root");
    }

    Object put(String path, Object value) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }

        String[] segments = split(path);
        Map<String, Object> current = root;

        for (int i = 0; i < segments.length - 1; i++) {
            String segment = segments[i];
            Object child = current.get(segment);
            if (!(child instanceof Map<?, ?> childMap)) {
                Map<String, Object> newMap = new ConcurrentHashMap<>();
                current.put(segment, newMap);
                current = newMap;
            } else {
                current = cast(childMap);
            }
        }

        return current.put(segments[segments.length - 1], value);
    }

    Object get(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        String[] segments = split(path);
        Map<String, Object> current = root;

        for (int i = 0; i < segments.length; i++) {
            Object value = current.get(segments[i]);
            if (value == null) {
                return null;
            }
            if (i == segments.length - 1) {
                return value;
            }
            if (value instanceof Map<?, ?> next) {
                current = cast(next);
            } else {
                return null;
            }
        }

        return null;
    }

    boolean has(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        String[] segments = split(path);
        Map<String, Object> current = root;

        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            if (!current.containsKey(segment)) {
                return false;
            }
            Object value = current.get(segment);
            if (i == segments.length - 1) {
                return true;
            }
            if (value instanceof Map<?, ?> next) {
                current = cast(next);
            } else {
                return false;
            }
        }
        return false;
    }

    Object remove(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        String[] segments = split(path);
        List<Map<String, Object>> ancestry = new ArrayList<>(segments.length);
        ancestry.add(root);
        Map<String, Object> current = root;

        for (int i = 0; i < segments.length - 1; i++) {
            Object value = current.get(segments[i]);
            if (!(value instanceof Map<?, ?> next)) {
                return null;
            }
            current = cast(next);
            ancestry.add(current);
        }

        Object removed = current.remove(segments[segments.length - 1]);
        cleanupEmptyAncestors(segments, ancestry);
        return removed;
    }

    private void cleanupEmptyAncestors(String[] segments, List<Map<String, Object>> ancestry) {
        for (int i = ancestry.size() - 1; i > 0; i--) {
            Map<String, Object> node = ancestry.get(i);
            if (!node.isEmpty()) {
                break;
            }
            Map<String, Object> parent = ancestry.get(i - 1);
            parent.remove(segments[i - 1]);
        }
    }

    private String[] split(String path) {
        return path.split("\\.");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> cast(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }
}
