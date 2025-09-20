package org.phong.zenflow.plugin.subdomain.registry.definitions.google;

import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;

/**
 * Handles Google OAuth consent URL generation and authorization code exchange for refresh tokens.
 */
@Slf4j
@Component
public class GoogleOAuthAuthorizationService {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKEN_SERVER_URL = "https://oauth2.googleapis.com/token";
    private static final String REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob";

    public String buildAuthorizationUrl(String clientId, Collection<String> scopes) {
        try {
            GoogleAuthorizationCodeRequestUrl requestUrl =
                    new GoogleAuthorizationCodeRequestUrl(clientId, REDIRECT_URI, scopes)
                            .setAccessType("offline");
            requestUrl.set("prompt", "consent");
            return requestUrl.build();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build Google authorization URL", ex);
        }
    }

    public String exchangeAuthorizationCode(
            String clientId,
            String clientSecret,
            String authorizationCode,
            Collection<String> scopes
    ) {
        try {
            HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
            GoogleAuthorizationCodeTokenRequest tokenRequest = new GoogleAuthorizationCodeTokenRequest(
                    transport,
                    JSON_FACTORY,
                    TOKEN_SERVER_URL,
                    clientId,
                    clientSecret,
                    authorizationCode,
                    REDIRECT_URI
            );
            tokenRequest.setScopes(scopes);
            TokenResponse response = tokenRequest.execute();
            String refreshToken = response.getRefreshToken();
            if (refreshToken == null || refreshToken.isBlank()) {
                throw new IllegalStateException("Google did not return a refresh token. Ensure offline access is granted.");
            }
            return refreshToken;
        } catch (TokenResponseException tre) {
            String error = tre.getDetails() != null ? tre.getDetails().getErrorDescription() : tre.getMessage();
            throw new IllegalStateException("Failed to exchange authorization code: " + error, tre);
        } catch (GeneralSecurityException | IOException ex) {
            throw new IllegalStateException("Failed to exchange authorization code with Google", ex);
        }
    }

    public String redirectUri() {
        return REDIRECT_URI;
    }
}
