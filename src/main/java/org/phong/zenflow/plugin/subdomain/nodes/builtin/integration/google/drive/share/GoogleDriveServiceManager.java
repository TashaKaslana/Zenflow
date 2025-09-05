package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.share;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.resource.BaseNodeResourceManager;
import org.phong.zenflow.workflow.subdomain.trigger.resource.TriggerResourceConfig;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Resource manager for Google Drive {@link Drive} clients.
 * Uses the generic {@link BaseNodeResourceManager} infrastructure for
 * sharing Drive instances across nodes.
 */
@Slf4j
@Component
public class GoogleDriveServiceManager extends BaseNodeResourceManager<Drive, TriggerResourceConfig> {

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    @Override
    protected Drive createResource(String resourceKey, TriggerResourceConfig config) {
        try {
            String clientId = config.getConfigValue("clientId", String.class);
            String clientSecret = config.getConfigValue("clientSecret", String.class);
            String refreshToken = config.getConfigValue("refreshToken", String.class);

            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            GoogleCredential credential = new GoogleCredential.Builder()
                    .setTransport(httpTransport)
                    .setJsonFactory(JSON_FACTORY)
                    .setClientSecrets(clientId, clientSecret)
                    .build();
            credential.setRefreshToken(refreshToken);

            return new Drive.Builder(httpTransport, JSON_FACTORY, credential)
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
