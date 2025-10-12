package org.phong.zenflow.workflow.subdomain.context;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;

class ExecutionContextLookupResolver {
    public static Object readFromCurrentConfig(String key, WorkflowConfig currentConfig, String currentNodeKey) {
        List<String> keys = resolveConfigLookupOrder(key, currentNodeKey);

        if (currentConfig == null || keys == null) {
            return null;
        }

        Map<String, Object> input = currentConfig.input();
        for (String candidateKey : keys) {
            if (candidateKey == null) {
                continue;
            }

            Object candidate = extractValue(input, candidateKey);
            if (candidate != null) {
                return candidate;
            }

            if ("profileKeys".equals(candidateKey) || "profile".equals(candidateKey)) {
                return currentConfig.profile();
            }

            if ("output".equals(candidateKey)) {
                return currentConfig.output();
            }
        }

        return null;
    }

    public static Object readFromRuntimeContext(String key, RuntimeContext context, String nodeKey) {
        if (context == null) {
            return null;
        }

        for (String candidate : resolveRuntimeLookupOrder(key, nodeKey)) {
            Object value = context.getAndClean(nodeKey, candidate);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public static List<String> resolveRuntimeLookupOrder(String key, String nodeKey) {
        if (key == null || key.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> lookup = new LinkedHashSet<>();
        addCandidate(lookup, scopeKey(key, nodeKey));
        addCandidate(lookup, key);
        return lookup.stream()
                .filter(candidate -> candidate != null && !candidate.isBlank())
                .toList();
    }
    
    private static Object extractValue(Map<String, Object> source, String path) {
        if (source == null || path == null) {
            return null;
        }

        if (!path.contains(".")) {
            return source.get(path);
        }

        String[] parts = path.split("\\.");
        Object current = source;
        for (String part : parts) {
            if (current instanceof Map<?, ?> currentMap) {
                current = currentMap.get(part);
            } else {
                return null;
            }

            if (current == null) {
                return null;
            }
        }

        return current;
    }



    private static List<String> resolveConfigLookupOrder(String key, String nodeKey) {
        if (key == null || key.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> lookup = new LinkedHashSet<>();
        addCandidate(lookup, stripScopePrefixes(scopeKey(key, nodeKey), nodeKey));
        addCandidate(lookup, stripScopePrefixes(key, nodeKey));
        return lookup.stream()
                .filter(candidate -> candidate != null && !candidate.isBlank())
                .toList();
    }

    private static String scopeKey(String key, String nodeKey) {
        if (key == null) {
            return null;
        }
        if (key.startsWith(ExecutionContextKey.PROHIBITED_KEY_PREFIX.key())) {
            return key;
        }
        if (ExecutionContextKey.fromKey(key).isPresent()) {
            return key;
        }
        if (nodeKey == null || nodeKey.isBlank()) {
            return key;
        }

        String nodePrefix = nodeKey + ".";
        if (key.startsWith(nodePrefix)) {
            String remainder = key.substring(nodePrefix.length());
            if (remainder.startsWith(ExecutionContextKey.INPUT_SCOPE_SEGMENT.key() + ".")) {
                return key;
            }
            if (remainder.startsWith("config.input.")) {
                return nodePrefix + ExecutionContextKey.INPUT_SCOPE_SEGMENT.key() + "." + remainder.substring("config.input.".length());
            }
            if (remainder.startsWith("config.")) {
                return nodePrefix + ExecutionContextKey.INPUT_SCOPE_SEGMENT.key() + "." + remainder.substring("config.".length());
            }
            return key;
        }

        if (key.startsWith(ExecutionContextKey.INPUT_SCOPE_SEGMENT.key() + ".")) {
            return nodePrefix + key;
        }

        if (key.startsWith("config.input.")) {
            return nodePrefix + ExecutionContextKey.INPUT_SCOPE_SEGMENT.key() + "." + key.substring("config.input.".length());
        }

        if (key.startsWith("config.")) {
            return nodePrefix + ExecutionContextKey.INPUT_SCOPE_SEGMENT.key() + "." + key.substring("config.".length());
        }

        return nodePrefix + ExecutionContextKey.INPUT_SCOPE_SEGMENT.key() + "." + key;
    }

    private static String stripScopePrefixes(String key, String nodeKey) {
        if (key == null) {
            return null;
        }
        String current = key;
        if (nodeKey != null && !nodeKey.isBlank()) {
            String nodePrefix = nodeKey + ".";
            if (current.startsWith(nodePrefix)) {
                current = current.substring(nodePrefix.length());
            }
        }
        current = stripPrefix(current, "config.");
        current = stripPrefix(current, ExecutionContextKey.INPUT_SCOPE_SEGMENT.key() + ".");
        return current;
    }

    private static String stripPrefix(String value, String prefix) {
        if (value == null) {
            return null;
        }
        if (value.startsWith(prefix)) {
            return value.substring(prefix.length());
        }
        return value;
    }

    private static void addCandidate(LinkedHashSet<String> lookup, String candidate) {
        if (candidate != null && !candidate.isBlank()) {
            lookup.add(candidate);
        }
    }
}