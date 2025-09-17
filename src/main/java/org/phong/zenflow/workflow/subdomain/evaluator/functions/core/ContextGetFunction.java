package org.phong.zenflow.workflow.subdomain.evaluator.functions.core;

import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.function.FunctionUtils;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.runtime.type.AviatorRuntimeJavaType;
import lombok.AllArgsConstructor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContextManager;
import org.phong.zenflow.workflow.subdomain.evaluator.functions.AviatorFunction;
import org.phong.zenflow.workflow.subdomain.node_definition.constraints.WorkflowConstraints;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Bridge function allowing expressions to read values from the
 * {@code ExecutionContext} via {@code get("node.output")}. The context is
 * expected to be provided in the evaluation environment under the key
 * {@code "context"}.
 */
@Component
@AviatorFunction
@AllArgsConstructor
public class ContextGetFunction extends AbstractFunction {
    private final static String PROHIBITED_KEY_PREFIX = "__zenflow_";
    private final RuntimeContextManager contextManager;

    @Override
    public String getName() {
        return "get";
    }

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1) {
        ExecutionContext ctx = (ExecutionContext) env.get("context");
        if (ctx == null) {
            return AviatorRuntimeJavaType.valueOf(null);
        }
        String key = FunctionUtils.getStringValue(arg1, env);

        if (key.startsWith(PROHIBITED_KEY_PREFIX)) {
            throw new IllegalArgumentException("Access to reserved context keys is prohibited: " + key);
        }

        Object value;
        String secretKey = stripReservedPrefix(key, WorkflowConstraints.RESERVED_SECRETS_PREFIX);
        if (secretKey != null) {
            value = ctx.getSecret(secretKey);
        } else {
            String profileKey = stripReservedPrefix(key, WorkflowConstraints.RESERVED_PROFILES_PREFIX);
            if (profileKey != null) {
                value = ctx.getProfileSecret(profileKey);
            } else {
                value = get(ctx, key);
            }
        }

        return AviatorRuntimeJavaType.valueOf(value);
    }

    private String stripReservedPrefix(String key, WorkflowConstraints constraint) {
        if (key == null) {
            return null;
        }

        String fullPrefix = constraint.key();
        if (key.startsWith(fullPrefix)) {
            return key.substring(fullPrefix.length());
        }

        String zenflowNamespace = WorkflowConstraints.ZENFLOW_PREFIX.key() + ".";
        if (fullPrefix.startsWith(zenflowNamespace)) {
            String aliasPrefix = fullPrefix.substring(zenflowNamespace.length());
            if (key.startsWith(aliasPrefix)) {
                return key.substring(aliasPrefix.length());
            }
        }

        return null;
    }

    public Object get(ExecutionContext ctx, String key) {
        RuntimeContext context = contextManager.getOrCreate(ctx.getWorkflowRunId().toString());

        if (context == null) return null;
        return context.getAndClean(ctx.getNodeKey(), key);
    }
}