package org.phong.zenflow.log.auditlog.annotations;

import org.phong.zenflow.log.auditlog.enums.AuditAction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {
    AuditAction action();
    String description() default "";
    String targetIdExpression() default "";
}
