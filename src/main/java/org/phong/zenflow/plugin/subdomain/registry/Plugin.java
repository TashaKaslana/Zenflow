package org.phong.zenflow.plugin.subdomain.registry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a plugin definition for automatic registration.
 * This annotation should be placed on classes that represent plugin metadata.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Plugin {

    /**
     * The unique key identifier for this plugin.
     * This should be unique across all plugins in the system.
     */
    String key();

    /**
     * The display name of the plugin.
     */
    String name();

    /**
     * The version of the plugin. This represents the bundle compound version.
     * When nodes are updated, this version should be incremented.
     */
    String version();

    /**
     * A description of what this plugin provides.
     */
    String description() default "";

    /**
     * Tags associated with this plugin for categorization and searchability.
     */
    String[] tags() default {};

    /**
     * Organization key for grouping related plugins under a vendor (e.g., "google").
     */
    String organization() default "";

    /**
     * The icon identifier for this plugin (e.g., "ph:core").
     */
    String icon() default "";

    /**
     * The URL of the plugin registry where this plugin is published.
     * Leave empty for built-in plugins.
     */
    String registryUrl() default "";

    /**
     * Whether this plugin is verified/trusted.
     */
    boolean verified() default true;

    /**
     * The publisher ID. For built-in plugins, this defaults to the system publisher.
     */
    String publisherId() default "00000000-0000-0000-0000-000000000000";
}
