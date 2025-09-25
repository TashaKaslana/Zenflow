package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.discord.executor;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.discord.core.DiscordJdaResourceManager;
import org.phong.zenflow.plugin.subdomain.resource.ScopedNodeResource;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.trigger.resource.DefaultTriggerResourceConfig;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Component
@PluginNode(
        key = "discord:message.send",
        name = "Discord Send Message",
        version = "1.0.0",
        description = "Sends a message to a Discord channel using JDA. Supports text messages and embeds.",
        type = "integration.message",
        tags = {"integration", "discord", "message", "send"},
        icon = "simple-icons:discord"
)
@Slf4j
@AllArgsConstructor
public class DiscordMessageExecutor implements PluginNodeExecutor {

    private final DiscordJdaResourceManager jdaResourceManager;
    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logs = context.getLogPublisher();

        String botToken;
        try {
            Map<String, Object> input = config.input();

            // Extract required parameters
            botToken = (String) context.getProfileSecret("BOT_TOKEN");
            String channelId = (String) input.get("channel_id");
            String message = (String) input.get("message");

            // Validate required parameters
            if (botToken == null || botToken.trim().isEmpty()) {
                return ExecutionResult.error("BOT_TOKEN is required");
            }
            if (channelId == null || channelId.trim().isEmpty()) {
                return ExecutionResult.error("channel_id is required");
            }
            if (message == null || message.trim().isEmpty()) {
                return ExecutionResult.error("message is required");
            }

            logs.info("Sending Discord message to channel: {}", channelId);

            Map<String, Object> configMap = new HashMap<>();
            configMap.put("BOT_TOKEN", botToken);
            DefaultTriggerResourceConfig resourceConfig = new DefaultTriggerResourceConfig(configMap, "BOT_TOKEN");

            try (ScopedNodeResource<JDA> handle = jdaResourceManager.acquire(botToken, context.getWorkflowRunId(), resourceConfig)) {
                JDA jda = handle.getResource();

                // Get the text channel
                TextChannel channel = jda.getTextChannelById(channelId);
                if (channel == null) {
                    logs.error("Channel not found or bot doesn't have access: {}", channelId);
                    return ExecutionResult.error("Channel not found or bot doesn't have access to channel: " + channelId);
                }

                // Create message
                MessageCreateData messageData = createMessage(input, message);

                // Send message and get response
                Message sentMessage = channel.sendMessage(messageData).complete();

                logs.success("Message sent successfully to channel: {}", channelId);

                // Return response with message details
                Map<String, Object> output = new HashMap<>();
                output.put("message_id", sentMessage.getId());
                output.put("channel_id", channelId);
                output.put("timestamp", sentMessage.getTimeCreated().toString());
                output.put("content", sentMessage.getContentDisplay());

                return ExecutionResult.success(output);
            }

        } catch (Exception e) {
            logs.withException(e).error("Failed to send Discord message: {}", e.getMessage());
            log.error("Discord message send error", e);
            return ExecutionResult.error("Failed to send Discord message: " + e.getMessage());
        }
    }

    /**
     * Creates a message with optional embed support
     */
    @SuppressWarnings("unchecked")
    private MessageCreateData createMessage(Map<String, Object> input, String message) {
        MessageCreateBuilder builder = new MessageCreateBuilder();
        builder.setContent(message);

        // Check if embed is requested
        Map<String, Object> embed = (Map<String, Object>) input.get("embed");
        if (embed != null) {
            MessageEmbed messageEmbed = createEmbed(embed);
            if (messageEmbed != null) {
                builder.setEmbeds(messageEmbed);
            }
        }

        return builder.build();
    }

    /**
     * Creates a Discord embed from configuration
     */
    @SuppressWarnings("unchecked")
    private MessageEmbed createEmbed(Map<String, Object> embedConfig) {
        try {
            net.dv8tion.jda.api.EmbedBuilder embedBuilder = new net.dv8tion.jda.api.EmbedBuilder();

            String title = (String) embedConfig.get("title");
            if (title != null) {
                embedBuilder.setTitle(title);
            }

            String description = (String) embedConfig.get("description");
            if (description != null) {
                embedBuilder.setDescription(description);
            }

            String color = (String) embedConfig.get("color");
            if (color != null) {
                embedBuilder.setColor(parseColor(color));
            }

            String thumbnail = (String) embedConfig.get("thumbnail");
            if (thumbnail != null) {
                embedBuilder.setThumbnail(thumbnail);
            }

            String image = (String) embedConfig.get("image");
            if (image != null) {
                embedBuilder.setImage(image);
            }

            String footer = (String) embedConfig.get("footer");
            if (footer != null) {
                embedBuilder.setFooter(footer);
            }

            // Add fields if present
            Object fieldsObj = embedConfig.get("fields");
            if (fieldsObj instanceof java.util.List) {
                java.util.List<Map<String, Object>> fields = (List<Map<String, Object>>) fieldsObj;
                for (Map<String, Object> field : fields) {
                    String name = (String) field.get("name");
                    String value = (String) field.get("value");
                    Boolean inline = (Boolean) field.getOrDefault("inline", false);

                    if (name != null && value != null) {
                        embedBuilder.addField(name, value, inline);
                    }
                }
            }

            return embedBuilder.build();

        } catch (Exception e) {
            log.warn("Failed to create embed, skipping: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parses color string to Color object
     */
    private Color parseColor(String colorStr) {
        try {
            if (colorStr.startsWith("#")) {
                return Color.decode(colorStr);
            } else if (colorStr.matches("\\d+")) {
                return new Color(Integer.parseInt(colorStr));
            } else {
                // Try to parse named colors
                return switch (colorStr.toLowerCase()) {
                    case "red" -> Color.RED;
                    case "green" -> Color.GREEN;
                    case "blue" -> Color.BLUE;
                    case "yellow" -> Color.YELLOW;
                    case "orange" -> Color.ORANGE;
                    case "pink" -> Color.PINK;
                    case "cyan" -> Color.CYAN;
                    case "magenta" -> Color.MAGENTA;
                    case "white" -> Color.WHITE;
                    case "black" -> Color.BLACK;
                    case "gray", "grey" -> Color.GRAY;
                    default -> Color.BLUE; // Default color
                };
            }
        } catch (Exception e) {
            log.warn("Failed to parse color '{}', using default blue", colorStr);
            return Color.BLUE;
        }
    }
}
