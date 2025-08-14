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
     * Type of the node (e.g. action, trigger, data).
     */
    String type() default "action";

    /**
     * Type of executor (e.g. builtin or remote).
     */
    String executor() default "builtin";
}
