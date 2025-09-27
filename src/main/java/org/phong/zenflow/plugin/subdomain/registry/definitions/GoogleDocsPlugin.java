package org.phong.zenflow.plugin.subdomain.registry.definitions;

import org.phong.zenflow.plugin.subdomain.registry.Plugin;
import org.phong.zenflow.plugin.subdomain.registry.profile.PluginProfileDescriptor;
import org.phong.zenflow.plugin.subdomain.registry.profile.PluginProfileProvider;
import org.phong.zenflow.plugin.subdomain.registry.definitions.google.GoogleOAuthProfileDescriptor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Google Docs plugin definition providing Google Docs nodes.
 */
@Component
@Plugin(
      key = "google-docs",
      name = "Google Docs Plugin",
      version = "1.0.0",
      description = "Plugin providing nodes that interact with Google Docs.",
      tags = {"google", "docs", "integration"},
      icon = "simple-icons:googledocs",
      organization = "google",
      schemaPath = "/google/plugin.schema.json"
)
public class GoogleDocsPlugin implements PluginProfileProvider {
    private final List<PluginProfileDescriptor> profiles;

    public GoogleDocsPlugin(GoogleOAuthProfileDescriptor descriptor) {
        this.profiles = List.of(descriptor);
    }

    @Override
    public List<PluginProfileDescriptor> getPluginProfiles() {
        return profiles;
    }
}
