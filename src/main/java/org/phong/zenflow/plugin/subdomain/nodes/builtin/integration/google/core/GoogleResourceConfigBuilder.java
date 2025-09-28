package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.core;

import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.trigger.resource.DefaultTriggerResourceConfig;

import java.util.HashMap;
import java.util.Map;

public class GoogleResourceConfigBuilder {

    public static DefaultTriggerResourceConfig build(ExecutionContext context) {
        String clientId = (String) context.getProfileSecret("CLIENT_ID");
        String clientSecret = (String) context.getProfileSecret("CLIENT_SECRET");
        String refreshToken = (String) context.getProfileSecret("CLIENT_REFRESH_TOKEN");

        if (clientId == null || clientSecret == null || refreshToken == null) {
            throw new GoogleCredentialsException("No valid Google OAuth profile found. Please ensure a Google profile is properly configured and linked to this node.");
        }

        Map<String, Object> cfg = new HashMap<>();
        cfg.put("clientId", clientId);
        cfg.put("clientSecret", clientSecret);
        cfg.put("refreshToken", refreshToken);
        return new DefaultTriggerResourceConfig(cfg, "refreshToken");
    }
}
