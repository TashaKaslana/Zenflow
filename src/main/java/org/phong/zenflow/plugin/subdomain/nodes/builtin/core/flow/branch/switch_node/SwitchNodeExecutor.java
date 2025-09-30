package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.branch.switch_node;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
@AllArgsConstructor
public class SwitchNodeExecutor implements NodeExecutor {
    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();
        try {
            Map<String, Object> input = config.input();

            if (!input.containsKey("expression")) {
                String errorMsg = "Switch expression is missing in the input.";
                logCollector.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            String value = input.get("expression") != null ? input.get("expression").toString() : null;
            List<SwitchCase> cases;

            try {
                cases = ObjectConversion.safeConvert(input.get("cases"), new TypeReference<>() {});
                logCollector.info("Begin switch flow with expression: {} and {} cases", value, cases.size());
            } catch (Exception e) {
                logCollector.withException(e).error("Failed to parse switch cases: {}", e.getMessage());
                return ExecutionResult.error("Invalid switch cases format");
            }

            if (value == null) {
                logCollector.warning("Switch expression is null");

                return getFallbackResult(logCollector, input);
            }

            for (SwitchCase c : cases) {
                if (c.value().equals(value)) {
                    logCollector.info("Found matching case for value: {} - proceeding to: {}", value, c.next());
                    return ExecutionResult.nextNode(c.next().getFirst());
                }
            }

            // No matches found, use a default case
            logCollector.info("No matching case found for value: {}", value);

            return getFallbackResult(logCollector, input);
        } catch (Exception e) {
            logCollector.withException(e).error("Failed to process switch-node: {}", e.getMessage());
            return ExecutionResult.error("Failed to process switch-node: " + e.getMessage());
        }
    }

    private ExecutionResult getFallbackResult(NodeLogPublisher logCollector, Map<String, Object> input) {
        if (!input.containsKey("default_case")) {
            logCollector.warning("No default case provided. Return null instead.");
            return ExecutionResult.nextNode(null);
        } else {
            String defaultCase = input.get("default_case") != null ? input.get("default_case").toString() : null;
            logCollector.info("Using default case: {}", defaultCase);
            return ExecutionResult.nextNode(defaultCase);
        }
    }
}
