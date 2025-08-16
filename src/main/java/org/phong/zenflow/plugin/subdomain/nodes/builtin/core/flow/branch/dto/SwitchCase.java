package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.branch.dto;

import java.util.List;

public record SwitchCase(String value, List<String> next) {
}
