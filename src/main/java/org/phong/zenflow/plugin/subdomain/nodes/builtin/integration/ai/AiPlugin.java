package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai;

import org.phong.zenflow.plugin.subdomain.registry.Plugin;
import org.springframework.stereotype.Component;

/**
 * Generic AI plugin for custom/unrecognized AI models
 * This serves as a base for models that don't have specific provider implementations
 */
@Component
@Plugin(
        key = "ai",
        name = "Custom AI",
        description = "Generic AI integration for custom or unrecognized AI models with text and JSON support",
        version = "1.0.0",
        tags = {"ai", "llm", "integration", "custom"},
        icon = "ph:brain",
        schemaPath = "./plugin.schema.json"
)
public class AiPlugin {
}
