package org.phong.zenflow.plugin.subdomain.executors.builtin.data_transformer.registry;

import org.phong.zenflow.plugin.subdomain.execution.exceptions.ExecutorException;
import org.phong.zenflow.plugin.subdomain.executors.builtin.data_transformer.interfaces.DataTransformer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class TransformerRegistry {
    private final Map<String, DataTransformer> registry;

    public TransformerRegistry(List<DataTransformer> plugins) {
        this.registry = plugins.stream().collect(Collectors.toMap(DataTransformer::getName, p -> p));
    }

    public DataTransformer getTransformer(String name) {
        return Optional.ofNullable(registry.get(name))
                .orElseThrow(() -> new ExecutorException("Unknown executor type: " + name));
    }
}
