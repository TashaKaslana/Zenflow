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
    }

    private void register(ProviderInfo info) {
        for (String alias : info.aliases()) {
            providers.put(alias.toLowerCase(Locale.ROOT), info);
        }
    }

    public Optional<ProviderInfo> resolve(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(providers.get(identifier.toLowerCase(Locale.ROOT)));
    }

    public ProviderInfo defaultProvider() {
        return defaultProvider;
    }

    public record ProviderInfo(String pluginKey, String nodeKey, String version, String executorType,
                               Set<String> aliases, String childAlias) {
    }
}
