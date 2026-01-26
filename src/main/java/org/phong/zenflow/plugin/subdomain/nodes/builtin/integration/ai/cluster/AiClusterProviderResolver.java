package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.cluster;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Helper that maps provider/model identifiers to provider metadata.
 */
@Component
public class AiClusterProviderResolver {

    private final Map<String, ProviderInfo> providers = new HashMap<>();
    private final Map<String, ProviderInfo> childAliasIndex = new HashMap<>();
    private final ProviderInfo defaultProvider;

    public AiClusterProviderResolver() {
        ProviderInfo gemini = new ProviderInfo(
                "google-ai",
                "gemini",
                "1.0.0",
                "builtin",
                Set.of("gemini", "google-ai:gemini", "gemini-2.0-flash", "gemini-1.5-pro", "gemini-1.5-flash"),
                "google-ai-gemini"
        );
        this.defaultProvider = gemini;
        register(gemini);

        ProviderInfo openai = new ProviderInfo(
                "openai",
                "chatgpt",
                "1.0.0",
                "builtin",
                Set.of("openai", "openai:chatgpt", "chatgpt", "gpt-4o", "gpt-4o-mini", "gpt-4.1", "gpt-3.5-turbo"),
                "openai-chatgpt"
        );
        register(openai);

        ProviderInfo openrouter = new ProviderInfo(
                "openrouter",
                "chat",
                "1.0.0",
                "builtin",
                Set.of("openrouter", "openrouter:chat", "openrouter/chat", "openrouter.ai"),
                "openrouter-chat"
        );
        register(openrouter);
    }

    private void register(ProviderInfo info) {
        for (String alias : info.aliases()) {
            providers.put(alias.toLowerCase(Locale.ROOT), info);
        }
        childAliasIndex.put(info.childAlias().toLowerCase(Locale.ROOT), info);
    }

    public Optional<ProviderInfo> resolve(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }
        String normalized = identifier.toLowerCase(Locale.ROOT);
        ProviderInfo direct = providers.get(normalized);
        if (direct != null) {
            return Optional.of(direct);
        }

        return providers.values().stream()
                .filter(info -> info.matches(normalized))
                .findFirst();
    }

    public Optional<ProviderInfo> resolveByChildAlias(String childAlias) {
        if (childAlias == null || childAlias.isBlank()) {
            return Optional.empty();
        }
        ProviderInfo info = childAliasIndex.get(childAlias.toLowerCase(Locale.ROOT));
        if (info != null) {
            return Optional.of(info);
        }
        return Optional.ofNullable(providers.get(childAlias.toLowerCase(Locale.ROOT)));
    }

    public ProviderInfo defaultProvider() {
        return defaultProvider;
    }

    public record ProviderInfo(String pluginKey, String nodeKey, String version, String executorType,
                               Set<String> aliases, String childAlias) {

        boolean matches(String identifier) {
            return aliases.contains(identifier);
        }
    }
}
