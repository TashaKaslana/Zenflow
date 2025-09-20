package org.phong.zenflow.plugin.subdomain.registry.definitions;

import org.phong.zenflow.plugin.subdomain.registry.Plugin;
import org.springframework.stereotype.Component;

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
  public class GoogleDocsPlugin {
    // Marker class for Google Docs plugin definition
}

