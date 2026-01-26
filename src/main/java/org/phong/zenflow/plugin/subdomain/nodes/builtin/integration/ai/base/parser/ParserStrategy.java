package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.parser;

import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto.AiExecutionResult;

/**
 * Post-provider parsing hook.
 */
public interface ParserStrategy {

    AiExecutionResult parse(AiExecutionResult rawResult);

    static ParserStrategy identity() {
        return raw -> raw;
    }
}
