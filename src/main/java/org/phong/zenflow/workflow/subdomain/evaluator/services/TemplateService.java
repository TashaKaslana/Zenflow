package org.phong.zenflow.workflow.subdomain.evaluator.services;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.AviatorEvaluatorInstance;
import com.googlecode.aviator.Expression;
import lombok.Getter;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.evaluator.PrefixFunctionEvaluator;
import org.phong.zenflow.workflow.subdomain.evaluator.functions.AviatorFunctionRegistry;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves templated expressions using Aviator. Custom functions are exposed
 * with the {@code fn:} prefix to avoid name collisions with workflow data.
 */
@Service
public class TemplateService {

    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\{\\{(.*?)}}");
    private static final Pattern REFERENCE_PATTERN = Pattern.compile("\\{\\{([a-zA-Z0-9_.\\-]+(?:\\([^)]*\\))?(?::[^}]*)?)}}");
    private static final Pattern REF_WITH_DEFAULT_PATTERN = Pattern.compile("^([a-zA-Z0-9_.\\-]+)(?::(.*))?$");

    private final AviatorEvaluatorInstance baseEvaluator;

    /**
     * -- GETTER --
     *  Exposes the shared evaluator as an immutable proxy. Callers must invoke
     * <p>
     *  to get a mutable copy before
     *  registering custom functions.
     */
    @Getter
    private final ImmutableEvaluator evaluator;
    private final AviatorFunctionRegistry functionRegistry;

    public TemplateService(AviatorFunctionRegistry functionRegistry) {
        this.functionRegistry = functionRegistry;
        this.baseEvaluator = AviatorEvaluator.newInstance();
        this.functionRegistry.registerAll(this.baseEvaluator);
        this.evaluator = new ImmutableEvaluator();
    }

    /**
     * Create a new evaluator instance that inherits all shared functions without
     * mutating the global evaluator. Custom functions registered on the returned
     * instance won't affect other executions.
     */
    public AviatorEvaluatorInstance newChildEvaluator() {
        AviatorEvaluatorInstance instance = AviatorEvaluator.newInstance();
        functionRegistry.registerAll(instance);
        return instance;
    }

    public boolean isTemplate(String value) {
        return value != null && EXPRESSION_PATTERN.matcher(value).find();
    }

    public Object resolve(String template, ExecutionContext context) {
        // Step 1: Ignore blank templates
        if (template == null || template.trim().isEmpty()) {
            return template;
        }

        Matcher matcher = EXPRESSION_PATTERN.matcher(template.trim());

        // Step 2: If the whole string is a single expression, keep the result type intact
        if (matcher.matches()) {
            String expression = matcher.group(1).trim();
            return evaluateExpression(expression, context);
        }

        // Step 3: Otherwise, resolve each embedded expression and build a final string
        StringBuilder sb = new StringBuilder();
        matcher.reset();
        while (matcher.find()) {
            String expression = matcher.group(1).trim();
            Object result = evaluateExpression(expression, context);
            matcher.appendReplacement(sb, result != null ? Matcher.quoteReplacement(result.toString()) : "null");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public Set<String> extractRefs(String template) {
        if (template == null) {
            return new LinkedHashSet<>();
        }

        Set<String> refs = new LinkedHashSet<>();
        Matcher matcher = REFERENCE_PATTERN.matcher(template);
        while (matcher.find()) {
            String fullExpression = matcher.group(1).trim();
            Matcher refMatcher = REF_WITH_DEFAULT_PATTERN.matcher(fullExpression);
            if (refMatcher.matches()) {
                String reference = refMatcher.group(1);
                if (!reference.matches("[a-zA-Z0-9._-]+\\(.*\\)")) {
                    refs.add(reference);
                }
            }
        }
        return refs;
    }

    public Set<String> extractRefs(Object value) {
        if (value instanceof String template) {
            return extractRefs(template);
        } else if (value instanceof Map<?, ?> map) {
            Set<String> refs = new LinkedHashSet<>();
            for (Object v : map.values()) {
                refs.addAll(extractRefs(v));
            }
            return refs;
        } else if (value instanceof List<?> list) {
            Set<String> refs = new LinkedHashSet<>();
            for (Object item : list) {
                refs.addAll(extractRefs(item));
            }
            return refs;
        } else if (value != null) {
            try {
                Map<?, ?> map = ObjectConversion.convertObjectToMap(value);
                return extractRefs(map);
            } catch (Exception e) {
                return extractRefs(value.toString());
            }
        }
        return new LinkedHashSet<>();
    }

    public String getReferencedNode(String templateExpression, Map<String, String> aliasMap) {
        if (templateExpression == null || templateExpression.isEmpty()) {
            return null;
        }

        if (aliasMap != null && !aliasMap.isEmpty()) {
            String actualTemplate = aliasMap.get(templateExpression);
            if (actualTemplate != null && actualTemplate.startsWith("{{") && actualTemplate.endsWith("}}")) {
                templateExpression = actualTemplate.substring(2, actualTemplate.length() - 2).trim();
            }
        }

        return templateExpression.split("\\.")[0];
    }

    private Object evaluateExpression(String expression, ExecutionContext context) {
        try {
            if (expression == null || expression.isEmpty()) {
                return null;
            }

            if (PrefixFunctionEvaluator.isFunction(expression)) {
                expression = PrefixFunctionEvaluator.stripPrefix(expression);
            } else {
                expression = String.format("get(\"%s\")", expression);
            }

            Expression compiledExp = baseEvaluator.compile(expression, true);
            return compiledExp.execute(Map.of("context", context));
        } catch (Exception e) {
            System.err.println("Failed to evaluate expression: " + expression + " - Error: " + e.getMessage());
            return "{{" + expression + "}}";
        }
    }

    /**
     * Read-only view of the shared evaluator. Mutation operations such as
     * {@code addFunction} are intentionally omitted. Consumers should call
     * {@link #cloneInstance()} to get a writable copy for custom functions.
     */
    public class ImmutableEvaluator {
        private ImmutableEvaluator() {
        }

        public Expression compile(String expression, boolean cached) {
            return baseEvaluator.compile(PrefixFunctionEvaluator.stripPrefix(expression), cached);
        }

        /**
         * Create a new evaluator instance populated with the shared functions.
         * The returned evaluator can be safely mutated by callers without
         * affecting other executions.
         */
        public AviatorEvaluatorInstance cloneInstance() {
            return newChildEvaluator();
        }
    }
}
