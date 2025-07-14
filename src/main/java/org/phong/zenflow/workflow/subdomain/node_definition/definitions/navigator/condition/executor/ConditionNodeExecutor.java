package org.phong.zenflow.workflow.subdomain.node_definition.definitions.navigator.condition.executor;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.aviator.AviatorEvaluator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.utils.TemplateEngine;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.navigator.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.navigator.condition.ConditionDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.navigator.condition.ConditionalCaseDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.navigator.condition.SwitchCaseDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.navigator.condition.SwitchDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
@AllArgsConstructor
public class ConditionNodeExecutor implements NodeExecutor<ConditionDefinition> {
    private final ObjectMapper mapper;

    @Override
    public ExecutionResult execute(ConditionDefinition node, Map<String, Object> context) {
        try {
            List<ConditionalCaseDefinition> cases = mapper.readValue((JsonParser) context.get("cases"), new TypeReference<>() {});
            for (ConditionalCaseDefinition caseDef: cases) {
                String rawCondition  = caseDef.when();
                String interpolated = TemplateEngine.resolveTemplate(rawCondition, context).toString();
                log.debug("Resolved condition: {}", interpolated);

                Boolean isMatch = (Boolean) AviatorEvaluator.execute(interpolated);
                if (isMatch) {
                    return ExecutionResult.nextNode(caseDef.then());
                }
            }
            return ExecutionResult.nextNode(null);
        } catch (Exception e) {
            log.error("Failed to parse cases from context", e);
            throw new RuntimeException("Invalid cases format in context", e);
        }
    }
}
