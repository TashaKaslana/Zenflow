package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.discord.trigger;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.hooks.EventListener;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.discord.share.DiscordJdaResourceManager;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerContext;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerExecutor;
import org.phong.zenflow.workflow.subdomain.trigger.resource.DefaultTriggerResourceConfig;
import org.phong.zenflow.plugin.subdomain.resource.NodeResourcePool;
import org.phong.zenflow.plugin.subdomain.resource.ScopedNodeResource;
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
    public Optional<NodeResourcePool<?, ?>> getResourceManager() {
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

        ScopedNodeResource<JDA> handle = jdaResourceManager.acquire(resourceKey, trigger.getId(), config);
        JDA jda = handle.getResource();

        // Attach the hub listener immediately (idempotent check)
        boolean hubAlreadyRegistered = jda.getRegisteredListeners().stream()
                .anyMatch(l -> l instanceof DiscordMessageListenerHub);
        if (!hubAlreadyRegistered) {
            jda.addEventListener(listenerHub);
            log.debug("Registered DiscordMessageListenerHub for key: {}", resourceKey);
        }

        jda.addEventListener((EventListener) event ->
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

        return new DiscordHubRunningHandle(resourceKey, channelId, trigger.getId(), handle, jdaResourceManager, listenerHub);
    }

    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        // This is called when the trigger fires (not for starting the trigger)
        Map<String, Object> output = new HashMap<>();
        output.put("trigger_type", "discord_message");
        output.put("triggered_at", System.currentTimeMillis());

        return ExecutionResult.success(output);
    }

    //    TODO:need optimization, this is draft pre-validate key, temporary disabled
//    @Override
    @SuppressWarnings("unused")
//    public List<ValidationError> validateDefinition(WorkflowConfig config) {
//        List<ValidationError> errors = new ArrayList<>();
//        String botToken = config.input().get("bot_token").toString();
//        if (botToken == null || botToken.isBlank()) {
//            errors.add(ValidationError.builder()
//                    .message("bot_token is required and must be a valid Discord bot token.")
//                    .path("bot_token")
//                    .errorCode(ValidationErrorCode.MISSING_REQUIRED_FIELD)
//                    .build());
//            return errors;
//        }
//
//        JDA jda = null;
//        try {
//            jda = JDABuilder.createDefault(botToken).build();
//        } catch (InvalidTokenException e) {
//            log.debug(e.getMessage());
//            errors.add(ValidationError.builder()
//                    .message("Invalid Discord bot token provided.")
//                    .path("bot_token")
//                    .errorCode(ValidationErrorCode.INVALID_INPUT_VALUE)
//                    .build());
//        }
//
//        String channelId = (String) config.input().get("channel_id");
//        if (channelId == null || channelId.isBlank()) {
//            errors.add(ValidationError.builder()
//                    .message("channel_id is required and must be a valid Discord channel ID.")
//                    .path("channel_id")
//                    .errorCode(ValidationErrorCode.MISSING_REQUIRED_FIELD)
//                    .build());
//        } else if (jda != null) {
//            try {
//                TextChannel channel = jda.getTextChannelById(channelId);
//            } catch (Exception e) {
//                log.debug(e.getMessage());
//                errors.add(ValidationError.builder()
//                        .message("channel_id does not correspond to a valid Discord text channel.")
//                        .path("channel_id")
//                        .errorCode(ValidationErrorCode.INVALID_INPUT_VALUE)
//                        .build());
//            } finally {
//                jda.shutdownNow();
//            }
//        }
//
//        return errors;
//    }

    private static class DiscordHubRunningHandle implements RunningHandle {
        private final String resourceKey;
        private final String channelId;
        private final UUID triggerId;
        private final ScopedNodeResource<JDA> handle;
        private final DiscordJdaResourceManager resourceManager;
        private final DiscordMessageListenerHub listenerHub;
        private volatile boolean running = true;

        public DiscordHubRunningHandle(String resourceKey, String channelId, UUID triggerId,
                                      ScopedNodeResource<JDA> handle,
                                      DiscordJdaResourceManager resourceManager,
                                      DiscordMessageListenerHub listenerHub) {
            this.resourceKey = resourceKey;
            this.channelId = channelId;
            this.triggerId = triggerId;
            this.handle = handle;
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
                handle.close();

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
