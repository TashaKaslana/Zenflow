package org.phong.zenflow.workflow.subdomain.node_definition.definitions.base.condition;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseNodeDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.enums.NodeType;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public final class ConditionDefinition extends BaseNodeDefinition {
    @NotNull
    private final List<ConditionalCaseDefinition> cases;

    @NotNull
    private final String defaultCase;

    public ConditionDefinition(String key, NodeType type, List<String> next, Map<String, Object> config,
                               Map<String, Object> metadata, Map<String, Object> policy,
                               List<ConditionalCaseDefinition> cases, String defaultCase) {
        super(key, type, next, config, metadata, policy);
        this.cases = cases;
        this.defaultCase = defaultCase;
    }
}
