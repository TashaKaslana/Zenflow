package org.phong.zenflow.plugin.subdomain.execution.dto;

import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;

/**
 * Input passed to node executors containing configuration and runtime metadata.
 */
public record ExecutionInput(WorkflowConfig config, RuntimeMetadata metadata) {
}
