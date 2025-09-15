package org.phong.zenflow.workflow.subdomain.node_definition;

import java.util.List;

public class WorkflowConstraints {
    public static String ZENFLOW_PREFIX = "zenflow";
    public static List<String> RESERVED_KEYS = List.of(
            ZENFLOW_PREFIX, "zenflow.secrets.", "zenflow.profiles"
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
}
