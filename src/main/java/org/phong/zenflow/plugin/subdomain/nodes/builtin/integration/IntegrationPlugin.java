package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration;

import org.phong.zenflow.plugin.subdomain.registry.Plugin;
import org.springframework.stereotype.Component;

/**
 * Integration plugin definition containing nodes for external service integrations.
 */
@Component
  @Plugin(
      key = "integration",
      name = "Integration Plugin",
      version = "1.0.0",
      description = "Plugin containing integration nodes for external services including databases, email, and third-party APIs.",
      tags = {"integration", "database", "email", "external"},
      icon = "ph:plug"
  )
  public class IntegrationPlugin {
    // This class serves as a marker for the integration plugin definition
    // The actual functionality is provided by the individual integration node executors
}
