package org.phong.zenflow.workflow.subdomain.node_definition.definitions.navigator.condition.executor;

import com.googlecode.aviator.AviatorEvaluator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.utils.TemplateEngine;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.navigator.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.navigator.condition.ConditionDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.navigator.condition.ConditionalCaseDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
@AllArgsConstructor
public class ConditionNodeExecutor implements NodeExecutor<ConditionDefinition> {
    @Override
    public String getNodeType() {
        return "condition";
    }

    @Override
    public ExecutionResult execute(ConditionDefinition node, Map<String, Object> context) {
        try {
            List<ConditionalCaseDefinition> cases = node.getCases();
            for (ConditionalCaseDefinition caseDef: cases) {
                String rawCondition  = caseDef.when();
                String interpolated = TemplateEngine.resolveTemplate(rawCondition, context).toString();
                if (interpolated == null || interpolated.isBlank()) {
                    throw new IllegalArgumentException("Node condition is null or blank after interpolation.");
                }
                log.debug("Resolved condition: {}", interpolated);

                Boolean isMatch = (Boolean) AviatorEvaluator.execute(interpolated);
                if (isMatch) {
                    return ExecutionResult.nextNode(caseDef.then());
                }
            }
            return ExecutionResult.nextNode(node.getDefaultCase());
        } catch (Exception e) {
            log.error("Failed to parse cases from context", e);
            throw new RuntimeException("Invalid cases format in context", e);
        }
    }
}
