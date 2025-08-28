package org.phong.zenflow.plugin.subdomain.node.registry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class as a plugin node definition. Metadata provided
 * here will be synchronized with the {@code plugin_nodes} table at runtime.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PluginNode {
    /**
     * Combined plugin and node key using the format {@code <pluginKey>:<nodeKey>}.
     */
    String key();

    String name();

    String version();

    String description() default "";

    String icon() default "";

    String[] tags() default {};

    /**
     * Type of the node (e.g., action, trigger, data).
     */
    String type() default "action";

    /**
     * Type of executor (e.g. builtin or remote).
     */
    String executor() default "builtin";

    /**
     * For trigger nodes, specifies the trigger type (manual, webhook, event, schedule, polling).
     * This field is optional and only used when type="trigger".
     */
    String triggerType() default "";

    /**
     * Custom path to the schema file for this node definition.
     * This is used to validate the node configuration against a JSON schema.
     */
    String schemaPath() default "";

    /**
     * Custom path to the documentation for this node definition.
     * This can be used to provide additional information or examples.
     */
    String docPath() default "";
}
