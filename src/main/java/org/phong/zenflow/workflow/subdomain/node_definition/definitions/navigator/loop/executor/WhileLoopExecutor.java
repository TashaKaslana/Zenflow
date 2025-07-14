package org.phong.zenflow.workflow.subdomain.node_definition.definitions.navigator.loop.executor;

import com.googlecode.aviator.AviatorEvaluator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.utils.TemplateEngine;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.navigator.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.navigator.loop.WhileLoopDefinition;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@AllArgsConstructor
@Slf4j
public class WhileLoopExecutor implements NodeExecutor<WhileLoopDefinition> {
    @Override
    public ExecutionResult execute(WhileLoopDefinition node, Map<String, Object> context) {
        String rawCondition = node.getCondition();
        String interpolated = TemplateEngine.resolveTemplate(rawCondition, context).toString();

        if (interpolated == null || interpolated.isBlank()) {
            throw new IllegalArgumentException("WhileLoop condition is null or blank after interpolation.");
        }

        Object result = AviatorEvaluator.execute(interpolated);
        if (!(result instanceof Boolean)) {
            throw new IllegalStateException("WhileLoop condition must evaluate to boolean, but got: " + result);
        }

        boolean isContinue = (Boolean) result;
        log.debug("WhileLoop evaluated condition [{}] to [{}]", interpolated, isContinue);

        if (node.getNext().isEmpty() || node.getLoopEnd().isEmpty()) {
            throw new IllegalStateException("WhileLoop node missing next or loopEnd target.");
        }

        return isContinue
                ? ExecutionResult.nextNode(node.getNext().getFirst())
                : ExecutionResult.nextNode(node.getLoopEnd().getFirst());
    }
}

