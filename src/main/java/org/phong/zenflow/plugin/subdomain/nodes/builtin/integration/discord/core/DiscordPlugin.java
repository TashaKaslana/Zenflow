package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.discord.core;

import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.discord.core.profile.DiscordProfileDescriptor;
import org.phong.zenflow.plugin.subdomain.registry.Plugin;
import org.phong.zenflow.plugin.subdomain.registry.profile.PluginProfileDescriptor;
import org.phong.zenflow.plugin.subdomain.registry.profile.PluginProfileProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Plugin(
        key = "discord",
        name = "Discord Plugin",
        version = "1.0.0",
        description = "Plugin providing nodes that interact with Discord.",
        tags = {"discord", "integration", "chat"},
        icon = "simple-icons:discord",
        organization = "discord"
)
public class DiscordPlugin implements PluginProfileProvider {
    private final List<PluginProfileDescriptor> profiles;

    public DiscordPlugin(DiscordProfileDescriptor profile) {
        this.profiles = List.of(profile);
    }

    @Override
    public List<PluginProfileDescriptor> getPluginProfiles() {
        return profiles;
    }
}
