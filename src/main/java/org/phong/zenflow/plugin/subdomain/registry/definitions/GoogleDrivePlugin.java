package org.phong.zenflow.plugin.subdomain.registry.definitions;

import org.phong.zenflow.plugin.subdomain.registry.Plugin;
import org.phong.zenflow.plugin.subdomain.registry.profile.PluginProfileDescriptor;
import org.phong.zenflow.plugin.subdomain.registry.profile.PluginProfileProvider;
import org.phong.zenflow.plugin.subdomain.registry.definitions.google.GoogleOAuthProfileDescriptor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Google Drive plugin definition providing Google Drive nodes.
 */
@Component
@Plugin(
      key = "google-drive",
      name = "Google Drive Plugin",
      version = "1.0.0",
      description = "Plugin providing nodes that interact with Google Drive.",
      tags = {"google", "drive", "integration"},
      icon = "simple-icons:googledrive",
      organization = "google",
      schemaPath = "/google/plugin.schema.json"
)
public class GoogleDrivePlugin implements PluginProfileProvider {
    private final List<PluginProfileDescriptor> profiles;

    public GoogleDrivePlugin(GoogleOAuthProfileDescriptor descriptor) {
        this.profiles = List.of(descriptor);
    }

    @Override
    public List<PluginProfileDescriptor> getPluginProfiles() {
        return profiles;
    }
}
