package org.phong.zenflow.plugin.subdomain.executors.builtin.triggers;

import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@Slf4j
public class WebhookTriggerExecutor implements PluginNodeExecutor {
    @Override
    public String key() {
        return "core:webhook.trigger:1.0.0";
    }

    @Override
    public ExecutionResult execute(WorkflowConfig config) {
        return ExecutionResult.success(Collections.emptyMap(), Collections.emptyList());
    }
}