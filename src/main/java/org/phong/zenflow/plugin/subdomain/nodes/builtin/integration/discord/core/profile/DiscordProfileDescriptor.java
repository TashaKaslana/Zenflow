package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.discord.core.profile;

import org.phong.zenflow.plugin.subdomain.registry.profile.PluginProfileDescriptor;
import org.springframework.stereotype.Component;

@Component
public class DiscordProfileDescriptor implements PluginProfileDescriptor {
    @Override
    public String id() {
        return "discord-bot-token";
    }

    @Override
    public String displayName() {
        return "Discord Profile";
    }

    @Override
    public String description() {
        return "Authenticate with Discord using a bot token and set following channel_id .";
    }

    @Override
    public String schemaPath() {
        return "./discord.profile.json";
    }
}
