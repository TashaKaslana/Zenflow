package org.phong.zenflow.workflow.subdomain.trigger.resource.specialized;

import org.phong.zenflow.workflow.subdomain.trigger.resource.BaseTriggerResourceManager;
import org.phong.zenflow.workflow.subdomain.trigger.resource.TriggerResourceConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Specialized resource manager for Discord JDA instances.
 * Unlike schedulers (which should be single shared instances), Discord connections
 * genuinely benefit from the GlobalDbConnectionPool pattern because:
 * - Different bot tokens require different JDA instances
 * - Multiple triggers can share the same bot token/JDA instance
 * - JDA instances are expensive to create and should be pooled
 * <p>
 * This is the CORRECT use of the resource pooling pattern.
 */
@Component
@Slf4j
public class DiscordResourceManager extends BaseTriggerResourceManager<Object> {

    @Override
    protected Object createResource(String resourceKey, TriggerResourceConfig config) {
        try {
            String botToken = config.getResourceIdentifier();
            String substring = botToken.substring(0, Math.min(8, botToken.length()));
            log.info("Creating new Discord JDA instance for bot token: {}...",
                    substring);

            // Create JDA instance here when Discord integration is added
            // JDA jda = JDABuilder.createDefault(botToken).build();
            // jda.awaitReady();

            // For now, return a placeholder
            Object jdaPlaceholder = new Object();
            log.info("Discord JDA instance created for token: {}...",
                    substring);

            return jdaPlaceholder;

        } catch (Exception e) {
            log.error("Failed to create Discord JDA instance: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create Discord JDA connection", e);
        }
    }

    @Override
    protected void cleanupResource(Object jda) {
        try {
            log.info("Shutting down Discord JDA instance");
            // jda.shutdown();
        } catch (Exception e) {
            log.error("Error shutting down Discord JDA: {}", e.getMessage(), e);
        }
    }

    @Override
    protected boolean checkResourceHealth(Object jda) {
        // return jda.getStatus() == JDA.Status.CONNECTED;
        return true; // Placeholder
    }
}
