package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.docs;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.DocsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.UserCredentials;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.resource.BaseNodeResourceManager;
import org.phong.zenflow.workflow.subdomain.trigger.resource.TriggerResourceConfig;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * Resource manager for Google Docs {@link Docs} clients.
 * Uses the generic {@link BaseNodeResourceManager} infrastructure for
 * sharing Docs instances across nodes. Credentials are sourced from
 * the plugin-level OAuth profile referenced by each node's {@code profile}
 * input.
 */
@Slf4j
@Component
public class GoogleDocsServiceManager extends BaseNodeResourceManager<Docs, TriggerResourceConfig> {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Override
    protected Docs createResource(String resourceKey, TriggerResourceConfig config) {
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

            return new Docs.Builder(
                    httpTransport,
                    JSON_FACTORY,
                    new HttpCredentialsAdapter(credentials.createScoped(List.of(DocsScopes.DOCUMENTS))))
                    .setApplicationName("Zenflow")
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            log.error("Failed to create Docs client: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create Docs client", e);
        }
    }

    @Override
    protected void cleanupResource(Docs docs) {
        // Docs clients do not require explicit cleanup
    }
}

