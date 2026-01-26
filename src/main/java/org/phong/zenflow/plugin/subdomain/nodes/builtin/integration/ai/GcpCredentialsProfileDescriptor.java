package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai;

import org.phong.zenflow.plugin.subdomain.registry.profile.PluginProfileDescriptor;
import org.springframework.stereotype.Component;

/**
 * Profile descriptor for AI provider credentials.
 * Works with any AI provider - just needs an API key.
 */
@Component
public class GcpCredentialsProfileDescriptor implements PluginProfileDescriptor {
    public static final String API_KEY = "API_KEY";
    public static final String BASE_URL = "BASE_URL";

    @Override
    public String id() {
        return "ai-credentials";
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
        return "/gcp.profile.schema.json";
    }
}
