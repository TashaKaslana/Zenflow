package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.core;

import com.google.api.services.docs.v1.DocsScopes;
import com.google.api.services.drive.DriveScopes;
import org.phong.zenflow.plugin.subdomain.registry.profile.PluginProfileDescriptor;
import org.phong.zenflow.plugin.subdomain.registry.profile.ProfilePreparationContext;
import org.phong.zenflow.plugin.subdomain.registry.profile.ProfilePreparationRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Shared Google OAuth profile descriptor used across Google plugins.
 */
@Component
public class GoogleOAuthProfileDescriptor implements PluginProfileDescriptor {

    static final String CLIENT_ID_KEY = "CLIENT_ID";
    static final String CLIENT_SECRET_KEY = "CLIENT_SECRET";
    static final String REFRESH_TOKEN_KEY = "CLIENT_REFRESH_TOKEN";
    static final String AUTHORIZATION_CODE_KEY = "AUTHORIZATION_CODE";

    private static final List<String> SCOPES = List.of(
            DriveScopes.DRIVE,
            DocsScopes.DOCUMENTS
    );

    private final GoogleOAuthAuthorizationService authorizationService;

    public GoogleOAuthProfileDescriptor(GoogleOAuthAuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Override
    public String id() {
        return "oauth-default";
    }

    @Override
    public String displayName() {
        return "Google OAuth Profile";
    }

    @Override
    public String description() {
        return "Uses a Google OAuth client to authenticate API calls.";
    }

    @Override
    public String schemaPath() {
        return "/google/oauth.profile.schema.json";
    }

    @Override
    public boolean requiresPreparation() {
        return true;
    }

    @Override
    public void prepareProfile(ProfilePreparationContext context) {
        Map<String, String> submitted = context.submittedValues();
        String clientId = submitted.get(CLIENT_ID_KEY);
        String clientSecret = submitted.get(CLIENT_SECRET_KEY);

        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalArgumentException("CLIENT_ID and CLIENT_SECRET are required to prepare the Google OAuth profile");
        }

        String existingRefresh = context.getGeneratedSecret(REFRESH_TOKEN_KEY);
        if (existingRefresh != null && !existingRefresh.isBlank()) {
            return; // Already generated
        }

        String authorizationCode = submitted.get(AUTHORIZATION_CODE_KEY);
        if (authorizationCode == null || authorizationCode.isBlank()) {
            //TODO: Add state verification in the future
            String state = UUID.randomUUID().toString();
            String authorizationUrl = authorizationService.buildAuthorizationUrl(clientId, SCOPES, state);
            context.requestAdditionalField(ProfilePreparationRequest.secretField(
                    AUTHORIZATION_CODE_KEY,
                    "Authorization Code",
                    "Open the Google consent URL, grant access, and paste the returned code.",
                    Map.of(
                            "authorizationUrl", authorizationUrl,
                            "redirectUri", authorizationService.redirectUri(),
                            "scopes", SCOPES
                    )
            ));
            return;
        }

        String refreshToken = authorizationService.exchangeAuthorizationCode(
                clientId,
                clientSecret,
                authorizationCode.trim(),
                SCOPES
        );

        context.putGeneratedSecret(REFRESH_TOKEN_KEY, refreshToken);
        context.clearRequestedField(AUTHORIZATION_CODE_KEY);
    }
}
