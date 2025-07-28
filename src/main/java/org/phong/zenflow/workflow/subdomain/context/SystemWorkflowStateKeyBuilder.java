package org.phong.zenflow.workflow.subdomain.context;

public class SystemWorkflowStateKeyBuilder {
    public static String loopState(String nodeKey) {
        return "__LOOP_STATE__:" + nodeKey;
    }

    public static String retryState(String nodeKey) {
        return "__RETRY_STATE__:" + nodeKey;
    }

    public static String conditionState(String nodeKey) {
        return "__CONDITION_STATE__:" + nodeKey;
    }

    public static String whileLoopState(String nodeKey) {
        return "__WHILE_LOOP_STATE__:" + nodeKey;
    }
}