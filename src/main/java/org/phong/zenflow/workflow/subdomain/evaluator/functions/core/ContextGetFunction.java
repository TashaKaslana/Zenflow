package org.phong.zenflow.workflow.subdomain.evaluator.functions.core;

import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.function.FunctionUtils;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.runtime.type.AviatorRuntimeJavaType;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
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

        Object value = ctx.get(key);
        return AviatorRuntimeJavaType.valueOf(value);
    }
}