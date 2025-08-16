package org.phong.zenflow.plugin.subdomain.nodes.builtin.data.data_transformer.impl.formatting;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.data.data_transformer.exception.DataTransformerExecutorException;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.data.data_transformer.interfaces.DataTransformer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@AllArgsConstructor
public class ToJsonTransformer implements DataTransformer {
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "to_json";
    }

    @Override
    public Object transform(Object data, Map<String, Object> params) {
        try {
            boolean pretty = (Boolean) (params != null ? params.getOrDefault("pretty", false) : false);
            boolean includeNulls = (Boolean) (params != null ? params.getOrDefault("includeNulls", true) : true);

            ObjectMapper mapper = objectMapper.copy();

            if (pretty) {
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
            }

            if (!includeNulls) {
                mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            }

            return mapper.writeValueAsString(data);

        } catch (Exception e) {
            throw new DataTransformerExecutorException("Error converting to JSON: " + e.getMessage());
        }
    }
}
