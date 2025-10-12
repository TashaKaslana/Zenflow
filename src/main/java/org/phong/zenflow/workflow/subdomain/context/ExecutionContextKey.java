package org.phong.zenflow.workflow.subdomain.context;

import java.util.Arrays;
import java.util.Optional;

public enum ExecutionContextKey {
    WORKFLOW_ID("__zenflow_workflow_id"),
    WORKFLOW_RUN_ID("__zenflow_workflow_run_id"),
    TRACE_ID("__zenflow_trace_id"),
    USER_ID("__zenflow_user_id"),
    CALLBACK_URL("__zenflow_callback_url"),
    NODE_KEY("__zenflow_node_key"),
    TRIGGER_ID("__zenflow_trigger_id"),
    TRIGGER_EXECUTOR_ID("__zenflow_trigger_executor_id"),
    PARENT_WORKFLOW_ID("__zenflow_parent_workflow_id"),
    ENTRYPOINT_LIST_KEY("__zenflow_entrypoint_list"),
    PROFILE_KEY("__zenflow__profiles"),
    SECRET_KEY("__zenflow_secrets"),
    
    PROHIBITED_KEY_PREFIX("__zenflow_"),
    INPUT_SCOPE_SEGMENT("input");

    private final String key;

    ExecutionContextKey(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public boolean matches(String candidate) {
        return key.equals(candidate);
    }

    @Override
    public String toString() {
        return key;
    }

    public static Optional<ExecutionContextKey> fromKey(String key) {
        return Arrays.stream(values())
                .filter(value -> value.matches(key))
                .findFirst();
    }
}
