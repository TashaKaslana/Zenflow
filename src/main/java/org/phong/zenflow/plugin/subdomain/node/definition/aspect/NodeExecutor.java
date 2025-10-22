package org.phong.zenflow.plugin.subdomain.node.definition.aspect;

import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;

public interface NodeExecutor {
    ExecutionResult execute(ExecutionContext context) throws Exception;
}
