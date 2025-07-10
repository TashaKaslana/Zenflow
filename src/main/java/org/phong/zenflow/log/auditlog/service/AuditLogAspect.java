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
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

@Aspect
@Component
@Slf4j
@AllArgsConstructor
public class AuditLogAspect {
    private final AuditLogService auditLogService;
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    private final AuthService authService;

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
                log.warn("User ID is null. Skipping activity logging because there isn't any user perform.");
                return;
            }

            HttpServletRequest request = HttpRequestUtils.getCurrentHttpRequest();
            String ipAddress = null;
            String userAgent = null;
            if (request != null) {
                ipAddress = HttpRequestUtils.getClientIpAddress(request);
                userAgent = request.getHeader("User-Agent");
            } else {
                log.warn("AOP: HttpServletRequest not found for method {} - IP/UA will be null.",
                        joinPoint.getSignature().getName()
                );
            }

            String description;

            if (logActivityAnnotation.description() != null && !logActivityAnnotation.description().trim().isEmpty()) {
                description = logActivityAnnotation.description();
            } else {
                description = generateDescription(joinPoint, logActivityAnnotation.action().getAction());
            }

            UUID targetId = null;
            String spelExpression = logActivityAnnotation.targetIdExpression();
            if (spelExpression != null && !spelExpression.trim().isEmpty()) {
                try {
                    targetId = evaluateSpel(joinPoint, result, spelExpression, UUID.class);
                } catch (Exception e) {
                    log.error("AOP: Failed to evaluate SpEL expression '{}' for targetId. Error: {}",
                            spelExpression, e.getMessage());
                }
            }

            Map<String, Object> activityDetail = null;
            //I have no idea about this, this is for update reference which could hold old value
            //change later, typical for updating or metadata
//          activityDetail = Map.of(
//                    "args", Arrays.toString(joinPoint.getArgs()),
//                    "result", result
//            );

            auditLogService.logActivity(
                    new CreateAuditLog(
                            currentUserId,
                            logActivityAnnotation.action().getAction(),
                            logActivityAnnotation.action().getTargetType().getValue(),
                            targetId,
                            description,
                            activityDetail,
                            userAgent != null ? userAgent.substring(0, Math.min(userAgent.length(), 30)) + "..." : null,
                            ipAddress
                    )
            );
        } catch (Exception e) {
            log.error("Failed to log activity after method successfully for method: {}, Error message: {}",
                    joinPoint.getSignature().getName(), e.getMessage()
            );
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

    private EvaluationContext createEvaluationContext(JoinPoint joinPoint, Object result) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setRootObject(joinPoint.getTarget()); // Set target object as root (less common usage)
        context.setVariable("result", result); // Make return value available as #result
        context.setVariable("target", joinPoint.getTarget()); // Make controller instance available as #target
        context.setVariable("args", joinPoint.getArgs()); // Make arguments array available as #args
        context.setVariable("currentUserId", authService.getUserIdFromContext()); // Make current user available as #currentUserId

        // Make individual method parameters available by name (requires -parameters javac flag or ParameterNameDiscoverer)
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

    //if not description is not provided, using this generated content
    private String generateDescription(JoinPoint joinPoint, String activityCode) {
        return String.format("User action [%s] completed. Args: %s",
                activityCode,
                Arrays.toString(joinPoint.getArgs())
        );
    }
}
