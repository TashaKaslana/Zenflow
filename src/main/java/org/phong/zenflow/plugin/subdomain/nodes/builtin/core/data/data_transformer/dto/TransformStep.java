package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransformStep {
    private String transformer;
    private Map<String, Object> params;
}