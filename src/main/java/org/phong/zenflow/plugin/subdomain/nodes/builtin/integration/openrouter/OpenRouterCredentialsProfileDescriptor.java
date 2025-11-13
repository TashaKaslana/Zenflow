package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.openrouter;

import org.phong.zenflow.plugin.subdomain.registry.profile.PluginProfileDescriptor;
import org.springframework.stereotype.Component;

/**
 * Profile descriptor for AI provider credentials.
 * Works with any AI provider - just needs an API key.
 */
@Component
public class OpenRouterCredentialsProfileDescriptor implements PluginProfileDescriptor {
    public static final String API_KEY = "API_KEY";

    @Override
    public String id() {
        return "openrouter-ai-credentials";
    }

    @Override
    public String displayName() {
        return "AI Provider Credentials";
    }

    @Override
    public String description() {
        return "API credentials for AI model providers (OpenAI, Gemini, Claude, etc.)";
    }

    @Override
    public String schemaPath() {
        return "/openrouter.profile.schema.json";
    }
}
