package org.phong.zenflow.plugin.subdomain.registry.profile;

import java.util.List;

/**
 * Implemented by plugin definition classes that expose credential profiles.
 */
public interface PluginProfileProvider {

    /**
     * @return ordered list of profile descriptors exposed by the plugin.
     */
    List<PluginProfileDescriptor> getPluginProfiles();
}
