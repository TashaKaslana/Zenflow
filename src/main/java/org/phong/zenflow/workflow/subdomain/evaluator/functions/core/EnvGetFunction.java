package org.phong.zenflow.workflow.subdomain.evaluator.functions.core;

import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.function.FunctionUtils;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.runtime.type.AviatorRuntimeJavaType;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.evaluator.functions.AviatorFunction;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@AviatorFunction
public class EnvGetFunction extends AbstractFunction {
    @Override
    public String getName() {
        return "getEnv";
    }

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1) {
        ExecutionContext ctx = (ExecutionContext) env.get("context");
        if (ctx == null) {
            return AviatorRuntimeJavaType.valueOf(null);
        }

        String key = FunctionUtils.getStringValue(arg1, env);

        Object value = switch (key) {
            case "WORKFLOW_ID" -> ctx.getWorkflowId();
            case "WORKFLOW_RUN_ID" -> ctx.getWorkflowRunId();
            case "TRACE_ID" -> ctx.getTraceId();
            case "USER_ID" -> ctx.getUserId();
            case "NODE_KEY" -> ctx.getNodeKey();
            default -> null;
        };

        return AviatorRuntimeJavaType.valueOf(value);
    }
}
