package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto.AiExecutionResult;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.parser.ParserConfig;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.parser.ParserStrategies;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.parser.ParserStrategy;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ParserStrategiesTest {

    private final ParserStrategies strategies = new ParserStrategies(new ObjectMapper());

    @Test
    void autoParsesJsonString() {
        AiExecutionResult raw = AiExecutionResult.builder()
                .rawResponse("{\"x\":1}")
                .output("{\"x\":1}")
                .parseSuccess(false)
                .build();

        ParserStrategy strategy = strategies.forConfig(ParserConfig.builder().strategy(ParserConfig.Strategy.AUTO).build());
        AiExecutionResult result = strategy.parse(raw);

        assertThat(result.getOutput()).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) result.getOutput()).get("x")).isEqualTo(1);
        assertThat(result.isParseSuccess()).isTrue();
    }

    @Test
    void jsonStrategyParsesRawString() {
        AiExecutionResult raw = AiExecutionResult.builder()
                .rawResponse("{\"a\":\"b\"}")
                .parseSuccess(false)
                .build();

        ParserStrategy strategy = strategies.forConfig(ParserConfig.builder().strategy(ParserConfig.Strategy.JSON).build());
        AiExecutionResult result = strategy.parse(raw);

        assertThat(result.getOutput()).isInstanceOf(Map.class);
        assertThat(result.isParseSuccess()).isTrue();
    }

    @Test
    void textStrategyReturnsString() {
        AiExecutionResult raw = AiExecutionResult.builder()
                .rawResponse("hello")
                .parseSuccess(false)
                .build();

        ParserStrategy strategy = strategies.forConfig(ParserConfig.builder().strategy(ParserConfig.Strategy.TEXT).build());
        AiExecutionResult result = strategy.parse(raw);

        assertThat(result.getOutput()).isEqualTo("hello");
        assertThat(result.isParseSuccess()).isTrue();
    }
}
