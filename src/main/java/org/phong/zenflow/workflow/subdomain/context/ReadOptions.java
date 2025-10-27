package org.phong.zenflow.workflow.subdomain.context;

/**
 * Options for controlling how context values are read.
 */
public enum ReadOptions {
    /**
     * Default behavior: Check config first, then runtime context.
     * This is the standard resolution order for most node executions.
     */
    DEFAULT,
    
    /**
     * Prioritize runtime context: Check runtime context first, then config.
     * Use this for loop executors where mutable state (index, loop variables) 
     * should take precedence over static config values.
     */
    PREFER_CONTEXT
}
