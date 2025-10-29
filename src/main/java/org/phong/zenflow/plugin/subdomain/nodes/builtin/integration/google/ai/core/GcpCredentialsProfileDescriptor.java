package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.ai.core;

import org.phong.zenflow.plugin.subdomain.registry.profile.PluginProfileDescriptor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Profile descriptor for GCP credentials used by Vertex AI.
 * Stores project ID, location, and optional service account key.
 */
@Component
public class GcpCredentialsProfileDescriptor implements PluginProfileDescriptor {

    public static final String PROJECT_ID_KEY = "GCP_PROJECT_ID";
    public static final String LOCATION_KEY = "GCP_LOCATION";
    public static final String SERVICE_ACCOUNT_KEY = "GCP_SERVICE_ACCOUNT_JSON";

    @Override
    public String id() {
        return "gcp-credentials";
    }

    @Override
    public String displayName() {
        return "GCP Credentials";
    }

    @Override
    public String description() {
        return "Google Cloud Platform credentials for Vertex AI access.";
    }

    @Override
    public String schemaPath() {
        return "/google/ai/gcp.profile.schema.json";
    }

    @Override
    public Map<String, Object> defaultValues() {
        return Map.of(
                LOCATION_KEY, "us-central1"
        );
    }
}
