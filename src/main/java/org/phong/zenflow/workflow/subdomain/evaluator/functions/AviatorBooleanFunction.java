package org.phong.zenflow.workflow.subdomain.evaluator.functions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for Aviator functions that return a boolean value.
 * <p>
 * Classes annotated with this will be auto-registered by
 * {@link AviatorFunctionRegistry}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AviatorBooleanFunction {
}
