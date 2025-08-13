package org.phong.zenflow.workflow.subdomain.engine.event;

import java.util.UUID;

/**
 * Event published when a workflow node transitions to COMMIT state.
 * Carries workflow and run identifiers along with the committed node key.
 */
public record NodeCommitEvent(UUID workflowId, UUID workflowRunId, String nodeKey) {}
