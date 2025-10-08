package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.UserCredentials;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.core.GoogleResourceConfigBuilder;
import org.phong.zenflow.plugin.subdomain.resource.BaseNodeResourceManager;
import org.phong.zenflow.plugin.subdomain.resource.ResourceConfig;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * Resource manager for Google Drive {@link Drive} clients.
 * Uses the generic {@link BaseNodeResourceManager} infrastructure for
 * sharing Drive instances across nodes. Credentials are sourced from
 * the plugin-level OAuth profile referenced by each node's {@code profile}
 * input.
 */
@Slf4j
@Component
public class GoogleDriveServiceManager extends BaseNodeResourceManager<Drive, ResourceConfig> {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Override
    public ResourceConfig buildConfig(WorkflowConfig cfg, ExecutionContext ctx) {
        return GoogleResourceConfigBuilder.build(ctx);
    }

    @Override
    protected Drive createResource(String resourceKey, ResourceConfig config) {
        try {
            String clientId = config.getConfigValue("clientId", String.class);
            String clientSecret = config.getConfigValue("clientSecret", String.class);
            String refreshToken = config.getConfigValue("refreshToken", String.class);

            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            GoogleCredentials credentials = UserCredentials.newBuilder()
                    .setClientId(clientId)
                    .setClientSecret(clientSecret)
                    .setRefreshToken(refreshToken)
                    .build();

            return new Drive.Builder(
                    httpTransport,
                    JSON_FACTORY,
                    new HttpCredentialsAdapter(credentials.createScoped(List.of(DriveScopes.DRIVE))))
                    .setApplicationName("Zenflow")
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            log.error("Failed to create Drive client: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create Drive client", e);
        }
    }

    @Override
    protected void cleanupResource(Drive drive) {
        // Drive clients do not require explicit cleanup
    }
}
