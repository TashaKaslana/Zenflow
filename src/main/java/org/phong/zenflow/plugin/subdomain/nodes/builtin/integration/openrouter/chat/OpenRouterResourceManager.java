package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.openrouter.chat;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiObservationRegistry;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiToolRegistry;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.openrouter.OpenRouterCredentialsProfileDescriptor;
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

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
@AllArgsConstructor
public class OpenRouterResourceManager extends BaseNodeResourceManager<OpenAiChatModel, OpenRouterResourceManager.OpenRouterConfig> {

    private static final String DEFAULT_BASE_URL = "https://openrouter.ai/api/v1";
    private static final String DEFAULT_COMPLETIONS_PATH = "/chat/completions";
    private static final String DEFAULT_EMBEDDINGS_PATH = "/embeddings";

    private final AiObservationRegistry observationRegistry;
    private final AiToolRegistry aiToolRegistry;

    @Override
    public OpenRouterConfig buildConfig(WorkflowConfig cfg, ExecutionContext ctx) {
        String apiKey = (String) ctx.getProfileSecret(OpenRouterCredentialsProfileDescriptor.API_KEY);
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("OpenRouter API requires an API_KEY secret in the ai-credentials profile.");
        }

        String baseUrl = ctx.readOrDefault("api_host", String.class, DEFAULT_BASE_URL);

        String model = ctx.readOrDefault("model", String.class, "openrouter/auto");
        String rawCompletionsPath = ctx.readOrDefault("api_completions_path", String.class, DEFAULT_COMPLETIONS_PATH);
        String rawEmbeddingsPath = ctx.readOrDefault("api_embeddings_path", String.class, DEFAULT_EMBEDDINGS_PATH);

        String normalizedBaseUrl = normalize(baseUrl);
        String normalizedCompletionsPath = alignPathWithBase(normalizedBaseUrl,
                normalizePath(rawCompletionsPath, DEFAULT_COMPLETIONS_PATH));
        String normalizedEmbeddingsPath = alignPathWithBase(normalizedBaseUrl,
                normalizePath(rawEmbeddingsPath, DEFAULT_EMBEDDINGS_PATH));

        return new OpenRouterConfig(
                apiKey,
                normalizedBaseUrl,
                model,
                normalizedCompletionsPath,
                normalizedEmbeddingsPath
        );
    }

    @Override
    protected OpenAiChatModel createResource(String resourceKey, OpenRouterConfig config) {
        log.info("Creating OpenRouter chat model for key: {}", resourceKey);

        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(config.apiKey())
                .baseUrl(config.baseUrl())
                .completionsPath(config.completionsPath())
                .embeddingsPath(config.embeddingsPath())
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
        log.info("Cleaning up OpenRouter chat model resource");
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

    private String normalize(String baseUrl) {
        String trimmed = baseUrl.trim();
        if (!trimmed.endsWith("/")) {
            trimmed = trimmed + "/";
        }
        return trimmed;
    }

    private String normalizePath(String candidate, String fallback) {
        String path = StringUtils.hasText(candidate) ? candidate.trim() : fallback;
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return path.replaceAll("/{2,}", "/");
    }

    private String alignPathWithBase(String baseUrl, String path) {
        try {
            URI uri = URI.create(baseUrl);
            String basePath = uri.getPath();
            if (!StringUtils.hasText(basePath)) {
                return path;
            }

            String cleanedBasePath = stripSurroundingSlashes(basePath);
            if (!StringUtils.hasText(cleanedBasePath)) {
                return path;
            }

            String normalizedPathBody = stripLeadingSlash(path);

            if (normalizedPathBody.startsWith(cleanedBasePath + "/")) {
                normalizedPathBody = normalizedPathBody.substring(cleanedBasePath.length() + 1);
            }

            List<String> baseSegments = Arrays.stream(cleanedBasePath.split("/"))
                    .filter(segment -> !segment.isBlank())
                    .toList();
            List<String> pathSegments = Arrays.stream(normalizedPathBody.split("/"))
                    .filter(segment -> !segment.isBlank())
                    .collect(Collectors.toList());

            if (baseSegments.isEmpty() || pathSegments.isEmpty()) {
                return rebuildPath(normalizedPathBody);
            }

            int overlap = 0;
            while (overlap < baseSegments.size() && overlap < pathSegments.size()) {
                String baseSegment = baseSegments.get(baseSegments.size() - overlap - 1);
                String pathSegment = pathSegments.get(overlap);
                if (!baseSegment.equals(pathSegment)) {
                    break;
                }
                overlap++;
            }

            if (overlap > 0) {
                pathSegments = pathSegments.subList(overlap, pathSegments.size());
                normalizedPathBody = String.join("/", pathSegments);
            }

            return rebuildPath(normalizedPathBody);
        } catch (IllegalArgumentException ex) {
            log.warn("Failed to align OpenRouter API path '{}' with base '{}': {}", path, baseUrl, ex.getMessage());
            return path;
        }
    }

    private String rebuildPath(String pathBody) {
        if (!StringUtils.hasText(pathBody)) {
            return "/";
        }
        return "/" + pathBody;
    }

    private String stripLeadingSlash(String value) {
        String result = value;
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        return result;
    }

    private String stripSurroundingSlashes(String value) {
        String result = value;
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    public record OpenRouterConfig(String apiKey,
                                   String baseUrl,
                                   String model,
                                   String completionsPath,
                                   String embeddingsPath) implements ResourceConfig {

        @Override
        public String getResourceIdentifier() {
            return baseUrl + model + apiKey.hashCode();
        }

        @Override
        public Map<String, Object> getContextMap() {
            return Map.of(
                    "base_url", baseUrl,
                    "model", model,
                    "completions_path", completionsPath,
                    "embeddings_path", embeddingsPath
            );
        }
    }
}
