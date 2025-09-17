package org.phong.zenflow.workflow.subdomain.node_definition.constraints;

import java.util.Arrays;

public enum WorkflowConstraints {
    ZENFLOW_PREFIX("zenflow"),
    RESERVED_SECRETS_PREFIX("zenflow.secrets."),
    RESERVED_PROFILES_PREFIX("zenflow.profiles.");

    private final String key;

    WorkflowConstraints(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public boolean matches(String candidate) {
        return candidate != null && (candidate.equals(key) || candidate.startsWith(key));
    }

    public static boolean isReservedKey(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        return Arrays.stream(values()).anyMatch(c -> c.matches(key));
    }

    public static String extractReservedKey(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(c -> c.matches(key))
                .map(WorkflowConstraints::key)
                .findFirst()
                .orElse(null);
    }
}
