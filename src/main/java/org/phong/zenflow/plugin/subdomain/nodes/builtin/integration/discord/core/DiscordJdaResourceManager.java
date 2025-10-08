package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.discord.core;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.phong.zenflow.plugin.subdomain.resource.BaseNodeResourceManager;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.trigger.resource.DefaultResourceConfig;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Discord JDA Resource Manager - handles sharing JDA instances across multiple triggers.
 * One JDA instance per Discord bot token, no matter how many workflow triggers use it.
 */
@Slf4j
@Component
public class DiscordJdaResourceManager extends BaseNodeResourceManager<JDA, DefaultResourceConfig> {
    private static final String BOT_TOKEN_KEY = "BOT_TOKEN";

    @Override
    public DefaultResourceConfig buildConfig(WorkflowConfig cfg, ExecutionContext ctx) {
        if (ctx == null) {
            throw new IllegalStateException("Execution context is required to build Discord resource configuration");
        }

        Object secret = ctx.getProfileSecret(BOT_TOKEN_KEY);
        if (!(secret instanceof String token) || token.isBlank()) {
            throw new IllegalStateException("BOT_TOKEN profile secret is required to acquire Discord JDA resource");
        }

        return new DefaultResourceConfig(Map.of(BOT_TOKEN_KEY, token), BOT_TOKEN_KEY);
    }

    @Override
    protected JDA createResource(String resourceKey, DefaultResourceConfig triggerConfig) {
        try {
            String botToken = triggerConfig.getResourceIdentifier();
            String redactedToken = botToken.length() > 8 ? botToken.substring(0, 8) : botToken;
            log.info("Creating new JDA instance for bot token: {}...", redactedToken);

            JDA jda = JDABuilder.createDefault(botToken)
                    .enableIntents(
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.MESSAGE_CONTENT,
                            GatewayIntent.DIRECT_MESSAGES
                    )
                    .build();

            log.info("JDA instance is building for token: {}...", redactedToken);
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
