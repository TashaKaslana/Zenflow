package org.phong.zenflow.plugin.subdomain.registry.definitions;

import org.phong.zenflow.plugin.subdomain.registry.Plugin;
import org.springframework.stereotype.Component;

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
    organization = "google"
)
public class GoogleDrivePlugin {
    // Marker class for Google Drive plugin definition
}
