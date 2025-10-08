package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.triggers.webhook;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "core:webhook.trigger",
        name = "Webhook Trigger",
        version = "1.0.0",
        description = "Executes when a webhook is triggered, processing the request data and metadata.",
        type = "trigger",
        triggerType = "webhook",
        tags = {"webhook", "trigger", "http", "api", "event" },
        icon = "ph:webhook"
)
public class WebhookTriggerNode implements NodeDefinitionProvider {
    private final WebhookTriggerExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
    }
}
