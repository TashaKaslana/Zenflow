package org.phong.zenflow.plugin.subdomain.registry.definitions.test;

import org.phong.zenflow.plugin.subdomain.registry.settings.PluginSettingDescriptor;

import java.util.Map;

public class TestSettingDescriptor implements PluginSettingDescriptor {
    @Override
    public String id() {
        return "test-settings";
    }

    @Override
    public String displayName() {
        return "Test Settings";
    }

    @Override
    public String description() {
        return "Sample plugin settings used for tests.";
    }

    @Override
    public String schemaPath() {
        return "/test/test.settings.schema.json";
    }

    @Override
    public Map<String, Object> defaultValues() {
        return Map.of("ENABLED", Boolean.TRUE);
    }
}
