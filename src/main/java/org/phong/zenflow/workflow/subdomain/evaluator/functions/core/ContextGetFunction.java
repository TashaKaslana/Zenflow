package org.phong.zenflow.workflow.subdomain.evaluator.functions.core;

import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.function.FunctionUtils;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.runtime.type.AviatorRuntimeJavaType;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.evaluator.functions.AviatorFunction;
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
public class ContextGetFunction extends AbstractFunction {
    private final static String PROHIBITED_KEY_PREFIX = "__zenflow_";

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
        if (key.startsWith("secrets.")) {
            String resolvedKey = key.substring("secrets.".length());
            value = ctx.getSecret(resolvedKey);
        } else if ( key.startsWith("profiles.")) {
            String resolvedKey = key.substring("profiles.".length());
            value = ctx.getProfileSecret(resolvedKey);
        } else {
            value = get(ctx, key);
        }

        return AviatorRuntimeJavaType.valueOf(value);
    }

    public Object get(ExecutionContext ctx, String key) {
        RuntimeContext context = (RuntimeContext) ctx.getContextManager()
                .get(ctx.getWorkflowRunId().toString());

        if (context == null) return null;
        return context.getAndClean(ctx.getNodeKey(), key);
    }
}