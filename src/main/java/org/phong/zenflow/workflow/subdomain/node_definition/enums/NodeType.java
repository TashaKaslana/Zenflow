package org.phong.zenflow.workflow.subdomain.node_definition.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum NodeType {
    PLUGIN("plugin"),
    CONDITION("condition"),
    SWITCH("switch"),
    IF("if"),
    WHILE_LOOP("while_loop"),
    FOR_LOOP("for_loop"),
    TIMEOUT("timeout"),
    START("start"),
    END("end");

    private final String type;
}
