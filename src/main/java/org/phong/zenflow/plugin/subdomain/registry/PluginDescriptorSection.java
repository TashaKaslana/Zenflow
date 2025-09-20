package org.phong.zenflow.plugin.subdomain.registry;

public enum PluginDescriptorSection {
    PROFILE,
    SETTING;

    public static PluginDescriptorSection from(String value) {
        if (value == null || value.isBlank()) {
            return PROFILE;
        }
        return switch (value.toLowerCase()) {
            case "profile" -> PROFILE;
            case "setting", "settings" -> SETTING;
            default -> throw new IllegalArgumentException("Unsupported descriptor section: " + value);
        };
    }
}
