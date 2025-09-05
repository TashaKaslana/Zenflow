package org.phong.zenflow.workflow.subdomain.evaluator.functions;

import com.googlecode.aviator.AviatorEvaluatorInstance;
import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.function.FunctionUtils;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.runtime.type.AviatorRuntimeJavaType;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Registers custom Aviator functions used by the workflow expression engine.
 * <p>
 * Boolean-returning functions annotated with {@link AviatorBooleanFunction} are
 * auto-discovered from the Spring context to reduce manual upkeep. All
 * discovered functions are exposed with the {@code fn:} prefix to avoid
 * clashing with user-provided identifiers.
 */
@Component
public class AviatorFunctionRegistry {

    private final List<AbstractFunction> booleanFunctions;

    public AviatorFunctionRegistry(List<AbstractFunction> functions) {
        this.booleanFunctions = functions.stream()
                .filter(f -> f.getClass().isAnnotationPresent(AviatorBooleanFunction.class))
                .toList();
    }

    /**
     * Register all built-in custom functions on the provided evaluator.
     *
     * @param evaluator the evaluator instance to configure
     */
    public void registerAll(AviatorEvaluatorInstance evaluator) {
        evaluator.addFunction(new ContextGetFunction());
        booleanFunctions.forEach(evaluator::addFunction);
    }

    /**
     * Bridge function allowing expressions to read values from the
     * {@code ExecutionContext} via {@code get("node.output")}. The context is
     * expected to be provided in the evaluation environment under the key
     * {@code "context"}.
     */
    private static class ContextGetFunction extends AbstractFunction {
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

}
