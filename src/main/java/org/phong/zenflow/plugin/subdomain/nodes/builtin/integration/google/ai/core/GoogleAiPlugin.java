package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.ai.core;

import org.phong.zenflow.plugin.subdomain.registry.Plugin;
import org.phong.zenflow.plugin.subdomain.registry.profile.PluginProfileDescriptor;
import org.phong.zenflow.plugin.subdomain.registry.profile.PluginProfileProvider;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Google AI plugin providing access to Vertex AI models (Gemini, etc.)
 */
@Component
@Plugin(
        key = "google-ai",
        name = "Google AI",
        description = "Google Vertex AI integration for Gemini and other Google AI models",
        version = "1.0.0",
        tags = {"google", "ai", "vertex-ai", "gemini", "llm"},
        icon = "simple-icons:google",
        organization = "google",
        schemaPath = "/google/ai/plugin.schema.json"
)
public class GoogleAiPlugin implements PluginProfileProvider {
    private final List<PluginProfileDescriptor> profiles;

    public GoogleAiPlugin(GcpCredentialsProfileDescriptor gcpDescriptor) {
        this.profiles = List.of(gcpDescriptor);
    }

    @Override
    public List<PluginProfileDescriptor> getPluginProfiles() {
        return profiles;
    }
}
