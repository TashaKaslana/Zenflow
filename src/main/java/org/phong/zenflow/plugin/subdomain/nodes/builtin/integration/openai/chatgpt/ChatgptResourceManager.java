package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.openai.chatgpt;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.GcpCredentialsProfileDescriptor;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiObservationRegistry;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiToolRegistry;
import org.phong.zenflow.plugin.subdomain.resource.BaseNodeResourceManager;
import org.phong.zenflow.plugin.subdomain.resource.ResourceConfig;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.ai.tool.resolution.StaticToolCallbackResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Map;

@Component
@Slf4j
@AllArgsConstructor
public class ChatgptResourceManager extends BaseNodeResourceManager<OpenAiChatModel, ChatgptResourceManager.OpenAiConfig> {

    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";

    private final AiObservationRegistry observationRegistry;
    private final AiToolRegistry aiToolRegistry;

    @Override
    public OpenAiConfig buildConfig(WorkflowConfig cfg, ExecutionContext ctx) {
        String apiKey = (String) ctx.getProfileSecret(GcpCredentialsProfileDescriptor.API_KEY);
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("OpenAI API requires an API_KEY secret in the ai-credentials profile.");
        }

        String baseUrl = ctx.read("api_host", String.class);
        if (!StringUtils.hasText(baseUrl)) {
            baseUrl = (String) ctx.getProfileSecret(GcpCredentialsProfileDescriptor.BASE_URL);
        }
        if (!StringUtils.hasText(baseUrl)) {
            baseUrl = DEFAULT_BASE_URL;
        }

        String model = ctx.readOrDefault("model", String.class, "gpt-4o-mini");

        return new OpenAiConfig(apiKey, normalize(baseUrl), model);
    }

    @Override
    protected OpenAiChatModel createResource(String resourceKey, ChatgptResourceManager.OpenAiConfig config) {
        log.info("Creating OpenAI chat model for key: {}", resourceKey);

        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(config.apiKey())
                .baseUrl(config.baseUrl())
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(config.model())
                .build();

        ToolCallingManager toolCallingManager = buildToolCallingManager(aiToolRegistry.copy());

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .toolCallingManager(toolCallingManager)
                .retryTemplate(RetryUtils.DEFAULT_RETRY_TEMPLATE)
                .observationRegistry(observationRegistry.getRegistry())
                .build();
    }

    @Override
    protected void cleanupResource(OpenAiChatModel resource) {
        log.info("Cleaning up OpenAI chat model resource");
    }

    private String normalize(String baseUrl) {
        String trimmed = baseUrl.trim();
        if (!trimmed.endsWith("/")) {
            trimmed = trimmed + "/";
        }
        return trimmed;
    }

    private ToolCallingManager buildToolCallingManager(AiToolRegistry registry) {
        ToolCallback[] toolCallbacks = MethodToolCallbackProvider.builder()
                .toolObjects(registry.getToolObjects())
                .build()
                .getToolCallbacks();

        return ToolCallingManager.builder()
                .toolCallbackResolver(new StaticToolCallbackResolver(Arrays.asList(toolCallbacks)))
                .observationRegistry(observationRegistry.getRegistry())
                .build();
    }

    public record OpenAiConfig(String apiKey, String baseUrl, String model) implements ResourceConfig {

        @Override
        public String getResourceIdentifier() {
            return baseUrl + model + apiKey.hashCode();
        }

        @Override
        public Map<String, Object> getContextMap() {
            return Map.of(
                    "base_url", baseUrl,
                    "model", model
            );
        }
    }
}
