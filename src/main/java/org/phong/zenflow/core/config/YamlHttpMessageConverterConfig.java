package org.phong.zenflow.core.config;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;

@Configuration
public class YamlHttpMessageConverterConfig {

    @Bean
    public AbstractJackson2HttpMessageConverter yamlHttpMessageConverter() {
        YAMLMapper yamlMapper = new YAMLMapper();
        yamlMapper.findAndRegisterModules();
        return new AbstractJackson2HttpMessageConverter(
                yamlMapper,
                MediaType.parseMediaType("application/x-yaml"),
                MediaType.parseMediaType("application/yaml"),
                MediaType.parseMediaType("text/yaml")
        ) {};
    }
}
