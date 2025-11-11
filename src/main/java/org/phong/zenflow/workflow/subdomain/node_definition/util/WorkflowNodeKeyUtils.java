package org.phong.zenflow.workflow.subdomain.node_definition.util;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility helpers for compound/materialized workflow node keys.
 */
public final class WorkflowNodeKeyUtils {

    public static final String SYNTHETIC_NODE_DELIMITER = "::";
    private static final Pattern ALLOWED_ALIAS = Pattern.compile("[^a-zA-Z0-9_-]+");

    private WorkflowNodeKeyUtils() {
    }

    /**
     * Builds a deterministic child key using the reserved delimiter.
     */
    public static String buildChildKey(String parentKey, String childAlias) {
        return parentKey + SYNTHETIC_NODE_DELIMITER + sanitizeAlias(childAlias);
    }

    /**
     * Strips invalid characters from a child alias so it can safely appear in node keys.
     */
    public static String sanitizeAlias(String alias) {
        if (alias == null || alias.isBlank()) {
            return "child";
        }
        String normalized = alias.trim().toLowerCase(Locale.ROOT);
        return ALLOWED_ALIAS.matcher(normalized).replaceAll("-");
    }

    /**
     * Returns true if the key represents a compound child (contains the delimiter).
     */
    public static boolean isCompoundChildKey(String key) {
        return key != null && key.contains(SYNTHETIC_NODE_DELIMITER);
    }

    /**
     * Extracts the parent portion from a compound child key.
     */
    public static String extractParentKey(String childKey) {
        if (!isCompoundChildKey(childKey)) {
            return null;
        }
        int idx = childKey.indexOf(SYNTHETIC_NODE_DELIMITER);
        return childKey.substring(0, idx);
    }
}
