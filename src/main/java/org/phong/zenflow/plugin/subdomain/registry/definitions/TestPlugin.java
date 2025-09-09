package org.phong.zenflow.plugin.subdomain.registry.definitions;

import org.phong.zenflow.plugin.subdomain.registry.Plugin;
import org.springframework.stereotype.Component;

/**
 * Test plugin definition containing nodes for testing and development purposes.
 */
@Component
  @Plugin(
      key = "test",
      name = "Test Plugin",
      version = "1.0.0",
      description = "Plugin containing test and development nodes including data generators, validators, and placeholder nodes.",
      tags = {"test", "development", "validation", "mock"},
      icon = "ph:test-tube"
  )
  public class TestPlugin {
    // This class serves as a marker for the test plugin definition
    // The actual functionality is provided by the individual test node executors
}
