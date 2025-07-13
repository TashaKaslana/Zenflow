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
public final class SwitchDefinition extends BaseNodeDefinition {
    @NotNull
    private final String compare;

    @NotNull
    private final List<SwitchCaseDefinition> cases;

    @NotNull
    private final String defaultCase;

    public SwitchDefinition(String key, NodeType type, List<String> next, Map<String, Object> config,
                            Map<String, Object> metadata, Map<String, Object> policy,
                            String compare, List<SwitchCaseDefinition> cases, String defaultCase) {
        super(key, type, next, config, metadata, policy);
        this.compare = compare;
        this.cases = cases;
        this.defaultCase = defaultCase;
    }
}
