package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.email;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "integration:email.send",
        name = "Send Email",
        version = "1.0.0",
        description = "Sends an email using configured SMTP settings. Supports HTML content and attachments.",
        type = "integration.communication",
        tags = {"integration", "email", "send", "smtp"},
        icon = "ph:envelope-simple"
)
public class EmailNode implements NodeDefinitionProvider {
    private final EmailExecutor executor;
    private final EmailResourceManager resourceManager;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .nodeResourceManager(resourceManager)
                .build();
    }
}
