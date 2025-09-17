package org.phong.zenflow.workflow.subdomain.node_definition.constraints;

import java.util.List;

public class WorkflowConstraints {
    public static String ZENFLOW_PREFIX = "zenflow";
    public static String RESERVED_SECRETS_PREFIX = "zenflow.secrets.";
    public static String RESERVED_PROFILES_PREFIX = "zenflow.profiles.";

    public static List<String> RESERVED_KEYS = List.of(
            ZENFLOW_PREFIX, RESERVED_SECRETS_PREFIX, RESERVED_PROFILES_PREFIX
    );

    public static boolean isReservedKey(String key) {
        if (key == null || key.isEmpty()) return false;
        for (String reserved : RESERVED_KEYS) {
            if (key.equals(reserved) || key.startsWith(reserved)) {
                return true;
            }
        }
        return false;
    }

    public static String extractReservedKey(String key) {
        if (key == null || key.isEmpty()) return null;
        for (String reserved : RESERVED_KEYS) {
            if (key.equals(reserved) || key.startsWith(reserved)) {
                return reserved;
            }
        }
        return null;
    }
}
