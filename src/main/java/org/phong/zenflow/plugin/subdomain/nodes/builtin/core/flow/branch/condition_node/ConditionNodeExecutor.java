package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.branch.condition_node;

import com.fasterxml.jackson.core.type.TypeReference;
import com.googlecode.aviator.AviatorEvaluator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_logs.utils.LogCollector;
import org.springframework.stereotype.Component;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
@PluginNode(
        key = "core:flow.branch.condition",
        name = "Condition Branch",
        version = "1.0.0",
        description = "Executes a branch based on conditions defined in the input. " +
                "Each case is evaluated in order, and the first matching case will be executed. " +
                "If no cases match, the default case will be executed if provided.",
        type = "flow",
        tags = {"core", "flow", "branch", "condition"},
        icon = "ph:git-branch"
)
@Slf4j
@AllArgsConstructor
public class ConditionNodeExecutor implements PluginNodeExecutor {
    private static final Pattern UNRESOLVED_PATTERN = Pattern.compile("\\{\\{[^}]+}}");

    @Override
    public String key() {
        return "core:flow.branch.condition:1.0.0";
    }

    @Override
    public ExecutionResult execute(WorkflowConfig config, RuntimeContext context) {
        LogCollector logCollector = new LogCollector();
        try {
            Map<String, Object> input = config.input();
            List<ConditionalCase> cases = ObjectConversion.safeConvert(input.get("cases"), new TypeReference<>() {});

            logCollector.info("Begin condition flow with cases: {}", cases.toString());

            for (ConditionalCase caseDef : cases) {
                String rawCondition = caseDef.when();
                if (rawCondition == null || rawCondition.isBlank()) {
                    throw new IllegalArgumentException("Node condition is null or blank.");
                }

                if (UNRESOLVED_PATTERN.matcher(rawCondition).find()) {
                    logCollector.warning("Skipping condition due to unresolved placeholder: " + rawCondition);
                    continue;
                }

                log.debug("Evaluating condition: {}", rawCondition);

                try {
                    Object result = AviatorEvaluator.execute(rawCondition);
                    Boolean isMatch = (Boolean) result;
                    if (Boolean.TRUE.equals(isMatch)) {
                        logCollector.info("Condition matched: {} than next to {}", rawCondition, caseDef.then());
                        return ExecutionResult.nextNode(caseDef.then(), logCollector.getLogs());
                    }
                } catch (Exception conditionException) {
                    log.warn("Condition evaluation failed for: {} - {}", rawCondition, conditionException.getMessage());
                    logCollector.warning(conditionException.getMessage());
                }
            }

            logCollector.info("No conditions matched, executing default case.");
            if (!input.containsKey("default_case")) {
                log.warn("No default case provided in the input.");
                logCollector.warning("No default case provided. Return null instead.");
                return ExecutionResult.nextNode(null, logCollector.getLogs());
            } else {
                return ExecutionResult.nextNode(input.get("default_case").toString(), logCollector.getLogs());
            }
        } catch (Exception e) {
            log.error("Failed to parse or evaluate cases", e);
            logCollector.error("Invalid cases format or condition expression: " + e.getMessage());
            return ExecutionResult.error("Invalid cases format or condition expression", logCollector.getLogs());
        }
    }
}