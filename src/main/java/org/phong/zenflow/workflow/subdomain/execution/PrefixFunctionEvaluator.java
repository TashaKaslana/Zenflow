package org.phong.zenflow.workflow.subdomain.execution;

public class PrefixFunctionEvaluator {
    public static final String FUNCTION_PREFIX = "fn:";

    public static boolean isFunction(String expression) {
        return expression != null && expression.startsWith(FUNCTION_PREFIX);
    }

    public static String stripPrefix(String expression) {
        if (isFunction(expression)) {
            return expression.substring(FUNCTION_PREFIX.length());
        }
        return expression;
    }
}
