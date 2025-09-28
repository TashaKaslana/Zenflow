package org.phong.zenflow.plugin.subdomain.nodes.builtin.core;

import org.phong.zenflow.plugin.subdomain.registry.Plugin;
import org.springframework.stereotype.Component;

/**
 * Core plugin definition containing essential built-in nodes for workflow execution.
 */
@Component
@Plugin(
    key = "core",
    name = "Core Plugin",
    version = "1.0.0",
    description = "Built-in core plugin containing essential nodes for workflow execution including HTTP requests, data transformation, flow control, loops, and triggers.",
    tags = {"core", "builtin", "essential"},
    icon = "ph:core"
)
public class CorePlugin {
    // This class serves as a marker for the core plugin definition
    // The actual functionality is provided by the individual node executors
}
