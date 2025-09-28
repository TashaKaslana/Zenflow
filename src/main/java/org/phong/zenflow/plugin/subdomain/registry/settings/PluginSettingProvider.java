package org.phong.zenflow.plugin.subdomain.registry.settings;

import java.util.List;

/**
 * Implemented by plugin definition classes that expose additional configuration sections.
 */
public interface PluginSettingProvider {

    List<PluginSettingDescriptor> getPluginSettings();
}
