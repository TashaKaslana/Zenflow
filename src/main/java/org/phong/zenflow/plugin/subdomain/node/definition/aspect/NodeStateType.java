package org.phong.zenflow.plugin.subdomain.node.definition.aspect;

import java.util.Set;

public enum NodeStateType {
    NORMAL,
    TOOL,
    AI_PROVIDER,
    MEMORY,
    PARSER,
    TRIGGER,
    DATABASE;

    public static boolean isAITool(NodeStateType stateType) {
        return stateType == TOOL || stateType == MEMORY;
    }

    public static boolean isAITool(Set<NodeStateType> stateTypeSet) {
        for (NodeStateType t : stateTypeSet) {
            if (isAITool(t)) {
                return true;
            }
        }

        return false;
    }
}
