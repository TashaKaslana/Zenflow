package org.phong.zenflow.workflow.subdomain.context.common;

import org.phong.zenflow.workflow.subdomain.context.ExecutionContextKey;

public class ContextKeyResolver {
    public static String normalizeKey(String nodeKey, String key) {
        if (key == null) {
            return null;
        }
        String normalized = key.trim();
        if (normalized.isEmpty()) {
            return normalized;
        }
        if (normalized.startsWith(ExecutionContextKey.PROHIBITED_KEY_PREFIX.key())) {
            return normalized;
        }
        if (nodeKey != null && !nodeKey.isBlank()) {
            String prefix = nodeKey + ".";
            if (normalized.startsWith(prefix)) {
                normalized = normalized.substring(prefix.length());
            }
        }

        return normalized;
    }

    public static String scopeKey(String nodeKey, String key) {
        return String.format("%s.output.%s", nodeKey, key);
    }
}
