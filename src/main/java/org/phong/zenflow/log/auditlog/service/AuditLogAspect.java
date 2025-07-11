package org.phong.zenflow.log.auditlog.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.phong.zenflow.core.services.AuthService;
import org.phong.zenflow.core.utils.HttpRequestUtils;
import org.phong.zenflow.log.auditlog.annotations.AuditLog;
import org.phong.zenflow.log.auditlog.dtos.CreateAuditLog;
import org.phong.zenflow.log.auditlog.events.AuditLogBatchEvent;
import org.phong.zenflow.log.auditlog.events.AuditLogEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Aspect
@Component
@Slf4j
@AllArgsConstructor
public class AuditLogAspect {
    private final AuditLogService auditLogService;
    private final AuthService authService;
    private final ApplicationEventPublisher publisher;

    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Pointcut("@annotation(logActivityAnnotation)")
    public void logActivityPointCut(AuditLog logActivityAnnotation) {
    }

    @AfterReturning(
            pointcut = "logActivityPointCut(logActivityAnnotation)",
            returning = "result",
            argNames = "joinPoint,logActivityAnnotation,result"
    )
    public void logAfterMethodExecutionSuccessfully(JoinPoint joinPoint, AuditLog logActivityAnnotation, Object result) {
        try {
            UUID currentUserId = authService.getUserIdFromContext();
            if (currentUserId == null) {
                log.warn("User ID is null. Skipping audit log.");
                return;
            }

            HttpServletRequest request = HttpRequestUtils.getCurrentHttpRequest();
            String ipAddress = request != null ? HttpRequestUtils.getClientIpAddress(request) : null;
            String userAgent = request != null ? request.getHeader("User-Agent") : null;

            String description = (logActivityAnnotation.description() != null && !logActivityAnnotation.description().trim().isEmpty())
                    ? logActivityAnnotation.description()
                    : generateDescription(joinPoint, logActivityAnnotation.action().getAction());

            String targetType = logActivityAnnotation.action().getTargetType().getValue();
            String action = logActivityAnnotation.action().getAction();
            String spel = logActivityAnnotation.targetIdExpression();

            if (spel == null || spel.isBlank()) {
                // No target ID expression, log the action
                auditLogService.logActivity(new CreateAuditLog(
                        currentUserId,
                        action,
                        targetType,
                        null,
                        description,
                        null,
                        shortenUserAgent(userAgent),
                        ipAddress
                ));
                return;
            }

            Object evalResult = evaluateSpel(joinPoint, result, spel, Object.class);

            if (evalResult instanceof Iterable<?> iterable) {
                // Bulk logging
                List<CreateAuditLog> logs = new ArrayList<>();
                for (Object item : iterable) {
                    UUID id = castToUUID(item);
                    logs.add(new CreateAuditLog(
                            currentUserId,
                            action,
                            targetType,
                            id,
                            description,
                            null,
                            shortenUserAgent(userAgent),
                            ipAddress
                    ));
                }
                publisher.publishEvent(new AuditLogBatchEvent(logs));
            } else {
                // Single log
                UUID targetId = castToUUID(evalResult);
                publisher.publishEvent(new AuditLogEvent(
                        buildAuditLog(
                                currentUserId,
                                action,
                                targetType,
                                targetId,
                                description,
                                userAgent,
                                ipAddress
                        )
                ));
            }

        } catch (Exception e) {
            log.error("Failed to log audit activity for method: {}", joinPoint.getSignature().getName(), e);
        }
    }

    private <T> T evaluateSpel(JoinPoint joinPoint, Object result, String expressionString, Class<T> desiredResultType) {
        try {
            Expression expression = parser.parseExpression(expressionString);
            EvaluationContext context = createEvaluationContext(joinPoint, result);
            return expression.getValue(context, desiredResultType);
        } catch (Exception e) {
            log.warn("AOP: SpEL evaluation failed for expression '{}': {}", expressionString, e.getMessage());
            return null;
        }
    }

    /**
     * Creates a Spring EvaluationContext for evaluating SpEL expressions in @AuditLog.
     * <p>
     * Supported expression variables:
     *
     * <ul>
     *     <li><b>#result</b>: The return value of the method (object or list)</li>
     *     <li><b>#args</b>: The full method arguments array</li>
     *     <li><b>#paramName</b>: Named parameters from method signature (e.g. #request, #ids)</li>
     *     <li><b>#target</b>: The object instance that the method was called on</li>
     *     <li><b>#currentUserId</b>: The current authenticated user ID (from context)</li>
     * </ul>
     * <p>
     * 💡 Example expressions:
     * <ul>
     *     <li>#result.id → for methods returning a single object with an `id`</li>
     *     <li>#result.![id] → for methods returning a list of objects with `id`</li>
     *     <li>#request.postId → for accessing a field inside a request param</li>
     *     <li>#ids → directly use a List&lt;UUID&gt; from method param</li>
     * </ul>
     * <p>
     * Ensure method parameter names are preserved using the `-parameters` compiler flag.
     */
    private EvaluationContext createEvaluationContext(JoinPoint joinPoint, Object result) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        // Core objects available to SpEL expressions
        context.setRootObject(joinPoint.getTarget());
        context.setVariable("result", result);
        context.setVariable("target", joinPoint.getTarget());
        context.setVariable("args", joinPoint.getArgs());
        context.setVariable("currentUserId", authService.getUserIdFromContext());

        // Add each named method parameter as a variable (e.g., #request, #ids, etc.)
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(signature.getMethod());
        Object[] args = joinPoint.getArgs();

        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }

        return context;
    }

    private CreateAuditLog buildAuditLog(UUID userId, String action, String targetType, UUID targetId, String description, String userAgent, String ipAddress) {
        return new CreateAuditLog(
                userId,
                action,
                targetType,
                targetId,
                description,
                null,
                shortenUserAgent(userAgent),
                ipAddress
        );
    }


    private String shortenUserAgent(String userAgent) {
        if (userAgent == null) return null;
        return userAgent.length() > 30 ? userAgent.substring(0, 30) + "..." : userAgent;
    }

    private UUID castToUUID(Object obj) {
        if (obj == null) return null;
        if (obj instanceof UUID uuid) return uuid;
        return UUID.fromString(obj.toString());
    }

    private String generateDescription(JoinPoint joinPoint, String activityCode) {
        log.debug("Audit log args: {}", Arrays.toString(joinPoint.getArgs()));
        return String.format("User action [%s] on method [%s]",
                activityCode,
                joinPoint.getSignature().getName()
        );
    }
}
