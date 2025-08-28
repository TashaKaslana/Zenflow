package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.discord;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerContext;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class DiscordMessageListenerHub extends ListenerAdapter {
    private final Map<String, DiscordMessageContext> listenerMap = new ConcurrentHashMap<>();

    public void addListener(String channelKey, DiscordMessageContext context) {
        listenerMap.put(channelKey, context);
        log.debug("Added Discord listener for channel: {}, workflow: {}", channelKey, context.workflowId());
    }

    public void removeListener(String channelKey) {
        DiscordMessageContext removed = listenerMap.remove(channelKey);
        if (removed != null) {
            log.debug("Removed Discord listener for channel: {}, workflow: {}", channelKey, removed.workflowId());
        }
    }

    public int getActiveListenerCount() {
        return listenerMap.size();
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        UUID triggerId = null;
        String channelId = event.getChannel().getId();

        try {
            DiscordMessageContext discordMessageContext = listenerMap.get(channelId);
            if (discordMessageContext == null) {
                // No listener registered for this channel - this is normal
                return;
            }

            // Apply filters from config
            if (!shouldTrigger(discordMessageContext.config(), event)) {
                return;
            }

            // Create payload with Discord event data
            Map<String, Object> payload = getPayload(event);

            // Start the workflow
            TriggerContext triggerContext = discordMessageContext.triggerContext();
            triggerId = discordMessageContext.triggerId();
            triggerContext.startWorkflow(discordMessageContext.workflowId(), payload);
            triggerContext.markTriggered(triggerId, Instant.now());

            log.debug("Discord trigger fired for message: {} in channel: {}", event.getMessageId(), channelId);

        } catch (Exception e) {
            log.error("Error handling Discord message for trigger {} in channel {}: {}",
                     triggerId, channelId, e.getMessage(), e);
        }
    }


    private boolean shouldTrigger(Map<String, Object> config, MessageReceivedEvent event) {
        String contentFilter = (String) config.get("content_contains");
        if (contentFilter != null && !event.getMessage().getContentDisplay().contains(contentFilter)) {
            return false;
        }

        Boolean ignoreBotsConfig = (Boolean) config.get("ignore_bots");
        return !Boolean.TRUE.equals(ignoreBotsConfig) || !event.getAuthor().isBot();
    }

    @NotNull
    private static Map<String, Object> getPayload(MessageReceivedEvent event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("message_id", event.getMessageId());
        payload.put("channel_id", event.getChannel().getId());
        payload.put("author_id", event.getAuthor().getId());
        payload.put("content", event.getMessage().getContentDisplay());
        event.getGuild();
        payload.put("guild_id", event.getGuild().getId());
        payload.put("timestamp", event.getMessage().getTimeCreated().toString());
        return payload;
    }

    public record DiscordMessageContext(UUID triggerId, UUID workflowId, Map<String, Object> config,
                                        TriggerContext triggerContext) {
    }
}
