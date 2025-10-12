package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.branch.condition_node;

import com.fasterxml.jackson.core.type.TypeReference;
import com.googlecode.aviator.AviatorEvaluatorInstance;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
@AllArgsConstructor
public class ConditionNodeExecutor implements NodeExecutor {
    @Override
    public ExecutionResult execute(ExecutionContext context) {
        NodeLogPublisher log = context.getLogPublisher();
        Object casesObj = context.read("cases", Object.class);
        List<ConditionalCase> cases = ObjectConversion.safeConvert(casesObj, new TypeReference<List<ConditionalCase>>() {});

        log.info("Begin condition flow with cases: {}", cases.toString());

        AviatorEvaluatorInstance evaluator = context.getEvaluator().cloneInstance();

        for (ConditionalCase caseDef : cases) {
            String rawCondition = caseDef.when();
            if (rawCondition == null || rawCondition.isBlank()) {
                throw new IllegalArgumentException("Node condition is null or blank.");
            }

            log.debug("Evaluating condition: {}", rawCondition);

            try {
                Object result = evaluator.execute(rawCondition, Map.of("context", context));
                Boolean isMatch = (Boolean) result;
                if (Boolean.TRUE.equals(isMatch)) {
                    log.info("Condition matched: {} than next to {}", rawCondition, caseDef.then());
                    return ExecutionResult.nextNode(caseDef.then());
                }
            } catch (Exception conditionException) {
                log.warn("Condition evaluation failed for: {} - {}", rawCondition, conditionException.getMessage());
                log.warning(conditionException.getMessage());
            }
        }

        log.info("No conditions matched, executing default case.");
        String defaultCase = context.read("default_case", String.class);
        if (defaultCase == null) {
            log.warn("No default case provided in the input.");
            log.warning("No default case provided. Return null instead.");
            return ExecutionResult.nextNode(null);
        } else {
            return ExecutionResult.nextNode(defaultCase);
        }
    }
}