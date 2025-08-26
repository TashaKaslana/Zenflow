package org.phong.zenflow.workflow.subdomain.node_definition.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public enum NodeType {
    PLUGIN("plugin"),
    CONDITION("condition"),
    SWITCH("switch"),
    IF("if"),
    WHILE_LOOP("while_loop"),
    FOR_LOOP("for_loop"),
    FOR_EACH("for_each"),
    TIMEOUT("timeout"),
    TRIGGER("trigger"),
    START("start"),
    END("end"),
    ACTION("action") //generic action node
    ;

    private final String type;

    public static NodeType fromString(String type) {
        for (NodeType nodeType : NodeType.values()) {
            if (nodeType.getType().equalsIgnoreCase(type)) {
                return nodeType;
            }
        }
        throw new IllegalArgumentException("Unknown node type: " + type);
    }

    public static List<NodeType> getLoopStatefulTypes() {
        return List.of(FOR_EACH, FOR_LOOP, WHILE_LOOP);
    }
}
