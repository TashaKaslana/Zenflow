package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.openai;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.GcpCredentialsProfileDescriptor;
import org.phong.zenflow.plugin.subdomain.registry.Plugin;
import org.phong.zenflow.plugin.subdomain.registry.profile.PluginProfileDescriptor;
import org.phong.zenflow.plugin.subdomain.registry.profile.PluginProfileProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Plugin(
        key = "openai",
        name = "OpenAI",
        description = "OpenAI supports various intelligent models",
        version = "1.0.0",
        tags = {"openai", "ai", "llm", "generate-content"},
        icon = "simple-icons:openai",
        organization = "openai"
)
@Component
@RequiredArgsConstructor
public class OpenAiPlugin implements PluginProfileProvider {
    private final GcpCredentialsProfileDescriptor gcpCredentialsProfileDescriptor;

    @Override
    public List<PluginProfileDescriptor> getPluginProfiles() {
        return List.of(gcpCredentialsProfileDescriptor);
    }
}
