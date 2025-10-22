package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.branch.switch_node;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@AllArgsConstructor
public class SwitchNodeExecutor implements NodeExecutor {
    @Override
    public ExecutionResult execute(ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();

        String value = context.read("expression", String.class);
        if (value == null) {
            String errorMsg = "Switch expression is missing in the input.";
            logCollector.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        List<SwitchCase> cases;
        try {
            Object casesObj = context.read("cases", Object.class);
            cases = ObjectConversion.safeConvert(casesObj, new TypeReference<List<SwitchCase>>() {});
            logCollector.info("Begin switch flow with expression: {} and {} cases", value, cases.size());
        } catch (Exception e) {
            logCollector.withException(e).error("Failed to parse switch cases: {}", e.getMessage());
            return ExecutionResult.error("Invalid switch cases format");
        }

        for (SwitchCase c : cases) {
            if (c.value().equals(value)) {
                logCollector.info("Found matching case for value: {} - proceeding to: {}", value, c.next());
                return ExecutionResult.nextNode(c.next().getFirst());
            }
        }

        // No matches found, use a default case
        logCollector.info("No matching case found for value: {}", value);

        return getFallbackResult(logCollector, context);
    }

    private ExecutionResult getFallbackResult(NodeLogPublisher logCollector, ExecutionContext context) {
        String defaultCase = context.read("default_case", String.class);
        if (defaultCase == null) {
            logCollector.warning("No default case provided. Return null instead.");
            return ExecutionResult.nextNode(null);
        } else {
            logCollector.info("Using default case: {}", defaultCase);
            return ExecutionResult.nextNode(defaultCase);
        }
    }
}
