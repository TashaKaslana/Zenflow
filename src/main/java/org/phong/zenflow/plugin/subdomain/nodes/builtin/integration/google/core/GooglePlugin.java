package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.core;

import org.phong.zenflow.plugin.subdomain.registry.Plugin;
import org.phong.zenflow.plugin.subdomain.registry.profile.PluginProfileDescriptor;
import org.phong.zenflow.plugin.subdomain.registry.profile.PluginProfileProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Plugin(
        key = "google",
        name = "Google Plugin",
        version = "1.0.0",
        description = "Plugin providing nodes that interact with Google services (Docs, Drive, etc.).",
        tags = {"google", "integration", "docs", "drive", "oauth"},
        icon = "simple-icons:google",
        organization = "google"
)
public class GooglePlugin implements PluginProfileProvider {
    private final List<PluginProfileDescriptor> profiles;

    public GooglePlugin(GoogleOAuthProfileDescriptor googleOAuthProfile) {
        this.profiles = List.of(googleOAuthProfile);
    }

    @Override
    public List<PluginProfileDescriptor> getPluginProfiles() {
        return profiles;
    }
}
