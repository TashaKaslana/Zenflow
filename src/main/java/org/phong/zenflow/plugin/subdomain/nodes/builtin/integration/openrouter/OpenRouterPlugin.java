package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.openrouter;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.registry.Plugin;
import org.phong.zenflow.plugin.subdomain.registry.profile.PluginProfileDescriptor;
import org.phong.zenflow.plugin.subdomain.registry.profile.PluginProfileProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Plugin(        
        key = "openrouter",
        name = "Openrouter",
        description = "OpenRouter supports various intelligent models",
        version = "1.0.0",
        tags = {"openrouter", "ai", "llm", "generate-content"},
        icon = "simple-icons:openrouter",
        organization = "openrouter"
)
@AllArgsConstructor
@Component
public class OpenRouterPlugin implements PluginProfileProvider {
    private final OpenRouterCredentialsProfileDescriptor descriptor;

    @Override
    public List<PluginProfileDescriptor> getPluginProfiles() {
        return List.of(descriptor);
    }
}
