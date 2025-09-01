package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.discord.trigger;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.discord.share.DiscordJdaResourceManager;
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
        description = "Listens for Discord messages and triggers workflows. Uses centralized hub for O(1) performance.",
        type = "trigger",
        triggerType = "event",
        tags = {"integration", "discord", "trigger", "message"},
        icon = "simple-icons:discord"
)
@Slf4j
@AllArgsConstructor
public class DiscordMessageTriggerExecutor implements TriggerExecutor {

    private final DiscordJdaResourceManager jdaResourceManager;
    private final DiscordMessageListenerHub listenerHub;

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

        JDA jda = jdaResourceManager.getOrCreateResource(resourceKey, config); // <-- capture the JDA

        // Register this trigger as using the resource
        jdaResourceManager.registerTriggerUsage(resourceKey, trigger.getId());

        // Attach the hub listener immediately (idempotent check)
        boolean hubAlreadyRegistered = jda.getRegisteredListeners().stream()
                .anyMatch(l -> l instanceof DiscordMessageListenerHub);
        if (!hubAlreadyRegistered) {
            jda.addEventListener(listenerHub);
            log.debug("Registered DiscordMessageListenerHub for key: {}", resourceKey);
        }

        jda.addEventListener((net.dv8tion.jda.api.hooks.EventListener) event ->
                log.info("JDA event: {}", event.getClass().getSimpleName())
        );

        // Extract channel ID from trigger config
        String channelId = (String) trigger.getConfig().get("channel_id");
        if (channelId == null) {
            throw new IllegalArgumentException("channel_id is required in trigger configuration");
        }

        // Create context for the hub
        DiscordMessageListenerHub.DiscordMessageContext context =
                new DiscordMessageListenerHub.DiscordMessageContext(
                        trigger.getId(),
                        trigger.getTriggerExecutorId(),
                        trigger.getWorkflowId(),
                        trigger.getConfig(),
                        ctx
                );

        // Register with the hub using channel ID as key
        listenerHub.addListener(channelId, context);

        log.info("Discord trigger started successfully for trigger: {} on channel: {}",
                trigger.getId(), channelId);

        return new DiscordHubRunningHandle(resourceKey, channelId, trigger.getId(), jdaResourceManager, listenerHub);
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
     * Running handle that manages the lifecycle of a Discord trigger using the hub approach
     */
    private static class DiscordHubRunningHandle implements RunningHandle {
        private final String resourceKey;
        private final String channelId;
        private final UUID triggerId;
        private final DiscordJdaResourceManager resourceManager;
        private final DiscordMessageListenerHub listenerHub;
        private volatile boolean running = true;

        public DiscordHubRunningHandle(String resourceKey, String channelId, UUID triggerId,
                                      DiscordJdaResourceManager resourceManager,
                                      DiscordMessageListenerHub listenerHub) {
            this.resourceKey = resourceKey;
            this.channelId = channelId;
            this.triggerId = triggerId;
            this.resourceManager = resourceManager;
            this.listenerHub = listenerHub;
        }

        @Override
        public void stop() {
            if (running) {
                running = false;

                // Remove from hub
                listenerHub.removeListener(channelId);

                // Unregister trigger usage (will cleanup JDA if no more triggers use it)
                resourceManager.unregisterTriggerUsage(resourceKey, triggerId);

                log.info("Discord trigger stopped: {} for channel: {}", triggerId, channelId);
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
