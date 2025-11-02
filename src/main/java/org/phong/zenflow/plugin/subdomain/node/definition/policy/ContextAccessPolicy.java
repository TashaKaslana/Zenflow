package org.phong.zenflow.plugin.subdomain.node.definition.policy;

/**
 * Controls how a node interacts with the workflow runtime context.
 *
 * <p>Nodes can opt into special handling such as persisting outputs even when
 * there are no registered downstream consumers. The default behaviour matches
 * existing semantics where outputs are only retained when explicitly consumed.
 */
public enum ContextAccessPolicy {
    /**
     * Standard behaviour. Pending writes are persisted only when there are
     * registered consumers and reads are consuming/garbage-collecting.
     */
    DEFAULT,

    /**
     * Persist outputs regardless of consumer analysis and favour non-destructive
     * reads for the node unless it explicitly overrides the write options.
     */
    PERSIST_OUTPUTS;

    public boolean forcePersistentWrites() {
        return this == PERSIST_OUTPUTS;
    }
}
