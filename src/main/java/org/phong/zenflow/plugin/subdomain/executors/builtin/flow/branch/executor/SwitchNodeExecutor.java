package org.phong.zenflow.plugin.subdomain.executors.builtin.flow.branch.executor;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.plugin.subdomain.executors.builtin.flow.branch.dto.SwitchCase;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class SwitchNodeExecutor implements PluginNodeExecutor {
    @Override
    public String key() {
        return "core:flow.branch.switch";
    }

    @Override
    public ExecutionResult execute(Map<String, Object> config, Map<String, Object> context) {
        String value = context.get(config.get("compare_value").toString()).toString();
        List<SwitchCase> cases = ObjectConversion.safeConvert(config.get("cases").toString(), new TypeReference<>() {});
        String defaultCase = config.get("default_case").toString();
        if (value == null) {
            log.warn("Switch compare value not found in context");
            return ExecutionResult.nextNode(defaultCase);
        }

        for (SwitchCase c : cases) {
            if (c.value().equals(value)) {
                return ExecutionResult.nextNode(c.next());
            }
        }
        return ExecutionResult.nextNode(defaultCase);
    }
}
