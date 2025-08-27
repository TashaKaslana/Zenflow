package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.discord;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerContext;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerExecutor;
import org.phong.zenflow.workflow.subdomain.trigger.resource.DefaultTriggerResourceConfig;
import org.phong.zenflow.workflow.subdomain.trigger.resource.TriggerResourceManager;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@PluginNode(
        key = "integration:discord.message.trigger",
        name = "Discord Message Trigger",
        version = "1.0.0",
        description = "Listens for Discord messages and triggers workflows. Shares JDA connections efficiently.",
        type = "trigger",
        tags = {"integration", "discord", "trigger", "message"},
        icon = "simple-icons:discord"
)
@Slf4j
@AllArgsConstructor
public class DiscordMessageTriggerExecutor implements TriggerExecutor {

    private final DiscordJdaResourceManager jdaResourceManager;

    @Override
    public String key() {
        return "integration:discord.message.trigger:1.0.0";
    }

    @Override
    public Optional<TriggerResourceManager<?>> getResourceManager() {
        return Optional.of(jdaResourceManager);
    }

    @Override
    public Optional<String> getResourceKey(WorkflowTrigger trigger) {
        // Use the Discord bot token as the resource key for sharing JDA instances
        String botToken = (String) trigger.getConfig().get("bot_token");
        return Optional.ofNullable(botToken);
    }

    @Override
    public RunningHandle start(WorkflowTrigger trigger, TriggerContext ctx) throws Exception {
        log.info("Starting Discord message trigger for workflow: {}", trigger.getWorkflowId());

        // Create resource config
        DefaultTriggerResourceConfig config = new DefaultTriggerResourceConfig(trigger, "bot_token");
        String resourceKey = config.getResourceIdentifier();

        // Get or create shared JDA instance
        JDA jda = jdaResourceManager.getOrCreateResource(resourceKey, config);

        // Register this trigger as using the resource
        jdaResourceManager.registerTriggerUsage(resourceKey, trigger.getId());

        // Create event listener for this specific trigger
        DiscordMessageListener listener = new DiscordMessageListener(
                trigger.getId(),
                trigger.getWorkflowId(),
                trigger.getConfig(),
                ctx
        );

        // Add listener to shared JDA instance
        jdaResourceManager.addEventListenerToJda(resourceKey, listener);

        log.info("Discord trigger started successfully for trigger: {}", trigger.getId());

        return new DiscordRunningHandle(resourceKey, listener, trigger.getId(), jdaResourceManager);
    }

    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        // This is called when the trigger fires (not for starting the trigger)
        Map<String, Object> output = new HashMap<>();
        output.put("trigger_type", "discord_message");
        output.put("triggered_at", System.currentTimeMillis());

        return ExecutionResult.success(output);
    }

    /**
     * Event listener that handles Discord messages for a specific workflow trigger
     */
    @AllArgsConstructor
    private static class DiscordMessageListener extends ListenerAdapter {
        private final UUID triggerId;
        private final UUID workflowId;
        private final Map<String, Object> config;
        private final TriggerContext triggerContext;

        @Override
        public void onMessageReceived(@NotNull MessageReceivedEvent event) {
            try {
                // Apply filters from config
                if (!shouldTrigger(event)) {
                    return;
                }

                // Create payload with Discord event data
                Map<String, Object> payload = getPayload(event);

                // Start the workflow
                triggerContext.startWorkflow(workflowId, payload);
                triggerContext.markTriggered(triggerId, java.time.Instant.now());

                log.debug("Discord trigger fired for message: {}", event.getMessageId());

            } catch (Exception e) {
                log.error("Error handling Discord message for trigger {}: {}", triggerId, e.getMessage(), e);
            }
        }

        private boolean shouldTrigger(MessageReceivedEvent event) {
            // Apply configuration-based filters
            String channelFilter = (String) config.get("channel_id");
            if (channelFilter != null && !channelFilter.equals(event.getChannel().getId())) {
                return false;
            }

            String contentFilter = (String) config.get("content_contains");
            if (contentFilter != null && !event.getMessage().getContentDisplay().contains(contentFilter)) {
                return false;
            }

            Boolean ignoreBotsConfig = (Boolean) config.get("ignore_bots");
            return !Boolean.TRUE.equals(ignoreBotsConfig) || !event.getAuthor().isBot();
        }
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

    /**
     * Running handle that manages the lifecycle of a Discord trigger
     */
    private static class DiscordRunningHandle implements RunningHandle {
        private final String resourceKey;
        private final DiscordMessageListener listener;
        private final UUID triggerId;
        private final DiscordJdaResourceManager resourceManager;
        private volatile boolean running = true;

        public DiscordRunningHandle(String resourceKey, DiscordMessageListener listener,
                                  UUID triggerId, DiscordJdaResourceManager resourceManager) {
            this.resourceKey = resourceKey;
            this.listener = listener;
            this.triggerId = triggerId;
            this.resourceManager = resourceManager;
        }

        @Override
        public void stop() {
            if (running) {
                running = false;

                // Remove listener from shared JDA
                resourceManager.removeEventListenerFromJda(resourceKey, listener);

                // Unregister trigger usage (will cleanup JDA if no more triggers use it)
                resourceManager.unregisterTriggerUsage(resourceKey, triggerId);

                log.info("Discord trigger stopped: {}", triggerId);
            }
        }

        @Override
        public boolean isRunning() {
            return running && resourceManager.isResourceHealthy(resourceKey);
        }

        @Override
        public String getStatus() {
            if (!running) return "STOPPED";
            return resourceManager.isResourceHealthy(resourceKey) ? "RUNNING" : "UNHEALTHY";
        }
    }
}
