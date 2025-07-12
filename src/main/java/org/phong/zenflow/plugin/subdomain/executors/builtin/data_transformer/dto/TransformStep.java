package org.phong.zenflow.plugin.subdomain.executors.builtin.data_transformer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class TransformStep {
    private String transformer;
    private Map<String, Object> params;
}