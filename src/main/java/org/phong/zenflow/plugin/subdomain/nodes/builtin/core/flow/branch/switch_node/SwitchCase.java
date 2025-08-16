package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.branch.switch_node;

import java.util.List;

public record SwitchCase(String value, List<String> next) {
}
