package org.phong.zenflow.plugin.subdomain.execution.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor}
 * implementation as a plugin node so it can be discovered from external JARs.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PluginNode {
    /**
     * The key of the plugin providing the node.
     */
    String plugin();

    /**
     * Unique key of the node inside the plugin.
     */
    String name();

    /**
     * Version of the node implementation.
     */
    String version() default "1.0.0";

    /**
     * Path to the configuration schema resource inside the JAR.
     */
    String schema() default "";

    /**
     * Executor type for the node (e.g. builtin, external).
     */
    String executorType() default "external";
}
