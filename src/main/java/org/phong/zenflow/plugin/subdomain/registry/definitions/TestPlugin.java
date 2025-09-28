package org.phong.zenflow.plugin.subdomain.registry.definitions;

import org.phong.zenflow.plugin.subdomain.registry.Plugin;
import org.phong.zenflow.plugin.subdomain.registry.definitions.test.TestSettingDescriptor;
import org.phong.zenflow.plugin.subdomain.registry.settings.PluginSettingDescriptor;
import org.phong.zenflow.plugin.subdomain.registry.settings.PluginSettingProvider;
import org.springframework.stereotype.Component;

import java.util.List;

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
public class TestPlugin implements PluginSettingProvider {
    private static final List<PluginSettingDescriptor> SETTINGS = List.of(new TestSettingDescriptor());

    @Override
    public List<PluginSettingDescriptor> getPluginSettings() {
        return SETTINGS;
    }
}
