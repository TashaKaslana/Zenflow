package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.discord.share;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.phong.zenflow.workflow.subdomain.trigger.resource.BaseTriggerResourceManager;
import org.phong.zenflow.workflow.subdomain.trigger.resource.TriggerResourceConfig;
import org.springframework.stereotype.Component;

/**
 * Discord JDA Resource Manager - handles sharing JDA instances across multiple triggers.
 * One JDA instance per Discord bot token, no matter how many workflow triggers use it.
 */
@Slf4j
@Component
public class DiscordJdaResourceManager extends BaseTriggerResourceManager<JDA> {

    @Override
    protected JDA createResource(String resourceKey, TriggerResourceConfig config) {
        try {
            String botToken = config.getResourceIdentifier();
            log.info("Creating new JDA instance for bot token: {}...", botToken.substring(0, 8));

            JDA jda = JDABuilder.createDefault(botToken)
                    .enableIntents(
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.MESSAGE_CONTENT,
                            GatewayIntent.DIRECT_MESSAGES
                    )
                    .build();

            log.info("JDA instance is building for token: {}...", botToken.substring(0, 8));
            return jda;

        } catch (Exception e) {
            log.error("Failed to create JDA instance: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create Discord JDA connection", e);
        }
    }

    @Override
    protected void cleanupResource(JDA jda) {
        try {
            log.info("Shutting down JDA instance");
            jda.shutdown();
        } catch (Exception e) {
            log.error("Error shutting down JDA: {}", e.getMessage(), e);
        }
    }

    @Override
    protected boolean checkResourceHealth(JDA jda) {
        return jda.getStatus() == JDA.Status.CONNECTED;
    }
}
