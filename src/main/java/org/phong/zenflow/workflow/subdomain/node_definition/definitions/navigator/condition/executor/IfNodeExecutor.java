package org.phong.zenflow.workflow.subdomain.node_definition.definitions.navigator.condition.executor;

import com.googlecode.aviator.AviatorEvaluator;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.utils.TemplateEngine;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.navigator.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.navigator.condition.IfDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class IfNodeExecutor implements NodeExecutor<IfDefinition> {
    @Override
    public String getNodeType() {
        return "if";
    }

    @Override
    public ExecutionResult execute(IfDefinition node, Map<String, Object> context) {
        String rawCondition = node.getCondition(); // e.g. "{{user.age}} > 18"

        // 1. Interpolate all templates first
        String interpolated = TemplateEngine.resolveTemplate(rawCondition, context).toString();
        if (interpolated == null || interpolated.isBlank()) {
            throw new IllegalArgumentException("If condition is null or blank after interpolation.");
        }
        log.debug("Resolved IF condition: {}", interpolated);

        // 2. Evaluate expression using Aviator
        boolean result;
        try {
            result = (Boolean) AviatorEvaluator.execute(interpolated);
        } catch (Exception e) {
            log.error("Failed to evaluate condition: {}", interpolated, e);
            throw new RuntimeException("Invalid condition expression: " + interpolated, e);
        }

        // 3. Branch
        String next = result
                ? getFirstOrNull(node.getNextTrue())
                : getFirstOrNull(node.getNextFalse());

        return ExecutionResult.nextNode(next);
    }

    private String getFirstOrNull(List<String> list) {
        return (list != null && !list.isEmpty()) ? list.getFirst() : null;
    }
}
