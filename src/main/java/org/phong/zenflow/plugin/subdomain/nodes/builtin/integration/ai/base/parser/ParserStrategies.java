package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto.AiExecutionResult;

import java.util.Map;

@RequiredArgsConstructor
@Slf4j
public class ParserStrategies {

    private final ObjectMapper objectMapper;

    public ParserStrategy forConfig(ParserConfig config) {
        if (config == null) {
            return ParserStrategy.identity();
        }
        return switch (config.getStrategy()) {
            case RAW -> ParserStrategy.identity();
            case TEXT -> this::asText;
            case JSON -> this::asJson;
            case SCHEMA, AUTO -> this::auto;
        };
    }

    private AiExecutionResult asText(AiExecutionResult input) {
        Object output = input.getOutput();
        if (output == null && input.getRawResponse() != null) {
            output = input.getRawResponse();
        }
        return input.toBuilder()
                .output(output != null ? output.toString() : null)
                .parseSuccess(true)
                .build();
    }

    private AiExecutionResult asJson(AiExecutionResult input) {
        Object output = input.getOutput();
        boolean success = input.isParseSuccess();
        if (!(output instanceof Map) && !(output instanceof Iterable)) {
            String raw = input.getRawResponse();
            if (raw != null) {
                try {
                    output = objectMapper.readValue(raw, Object.class);
                    success = true;
                } catch (Exception ex) {
                    log.warn("JSON parse failed: {}", ex.getMessage());
                    success = false;
                }
            } else {
                success = false;
            }
        }
        return input.toBuilder()
                .output(output)
                .parseSuccess(success)
                .build();
    }

    private AiExecutionResult auto(AiExecutionResult input) {
        // If already parsed, keep; if raw string, attempt JSON else leave as text.
        if (input.isParseSuccess() && (input.getOutput() instanceof Map || input.getOutput() instanceof Iterable)) {
            return input;
        }
        Object output = input.getOutput();
        boolean success = input.isParseSuccess();
        if (output == null && input.getRawResponse() != null) {
            output = input.getRawResponse();
        }
        if (output instanceof String rawStr) {
            try {
                Object parsed = objectMapper.readValue(rawStr, Object.class);
                return input.toBuilder()
                        .output(parsed)
                        .parseSuccess(true)
                        .build();
            } catch (Exception ex) {
                log.debug("Auto parser kept text; JSON parse failed: {}", ex.getMessage());
                return input.toBuilder()
                        .output(rawStr)
                        .parseSuccess(false)
                        .build();
            }
        }
        return input.toBuilder()
                .output(output)
                .parseSuccess(success)
                .build();
    }
}
