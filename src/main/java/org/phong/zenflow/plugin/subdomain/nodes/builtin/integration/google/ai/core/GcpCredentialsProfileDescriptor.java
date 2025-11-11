package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.ai.core;

import org.phong.zenflow.plugin.subdomain.registry.profile.PluginProfileDescriptor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Profile descriptor for AI provider credentials.
 * Works with any AI provider - just needs an API key.
 */
@Component
public class GcpCredentialsProfileDescriptor implements PluginProfileDescriptor {

    public static final String API_KEY = "API_KEY";
    public static final String BASE_URL = "BASE_URL";
    public static final String PROJECT_ID = "PROJECT_ID";
    public static final String REGION = "REGION";
    public static final String SERVICE_ACCOUNT_JSON = "SERVICE_ACCOUNT_JSON";
    public static final String CLIENT_ID = "CLIENT_ID";
    public static final String CLIENT_SECRET = "CLIENT_SECRET";
    public static final String REFRESH_TOKEN = "REFRESH_TOKEN";

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
        return "/google/ai/gcp.profile.schema.json";
    }

    @Override
    public Map<String, Object> defaultValues() {
        return Map.of(
                REGION, "us-central1"
        );
    }
}
