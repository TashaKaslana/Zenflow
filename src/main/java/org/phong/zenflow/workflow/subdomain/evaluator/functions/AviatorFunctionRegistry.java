package org.phong.zenflow.workflow.subdomain.evaluator.functions;

import com.googlecode.aviator.AviatorEvaluatorInstance;
import com.googlecode.aviator.runtime.function.AbstractFunction;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Registers custom Aviator functions used by the workflow expression engine.
 * <p>
 * Boolean-returning functions annotated with {@link AviatorFunction} are
 * auto-discovered from the Spring context to reduce manual upkeep. All
 * discovered functions are exposed with the {@code fn:} prefix to avoid
 * clashing with user-provided identifiers.
 */
@Component
public class AviatorFunctionRegistry {

    private final List<AbstractFunction> functions;

    public AviatorFunctionRegistry(List<AbstractFunction> functions) {
        this.functions = functions.stream()
                .filter(f -> f.getClass().isAnnotationPresent(AviatorFunction.class))
                .toList();
    }

    /**
     * Register all built-in custom functions on the provided evaluator.
     *
     * @param evaluator the evaluator instance to configure
     */
    public void registerAll(AviatorEvaluatorInstance evaluator) {
        functions.forEach(evaluator::addFunction);
    }
}
