package org.phong.zenflow.workflow.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Request object for updating a workflow definition.
 * Allows upserting nodes and removing nodes in a single request.
 */
public record WorkflowDefinitionChangeRequest(
        UpsertWorkflowDefinition upsert,
        List<String> remove
) implements Serializable {
}
