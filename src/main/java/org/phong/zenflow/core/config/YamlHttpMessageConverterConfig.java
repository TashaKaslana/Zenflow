package org.phong.zenflow.core.config;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class YamlHttpMessageConverterConfig implements WebMvcConfigurer {

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // Add YAML converter at the end of the list to ensure JSON takes precedence
        converters.add(yamlHttpMessageConverter());
    }

    private AbstractJackson2HttpMessageConverter yamlHttpMessageConverter() {
        YAMLMapper yamlMapper = new YAMLMapper();
        yamlMapper.findAndRegisterModules();
        return new AbstractJackson2HttpMessageConverter(
                yamlMapper,
                MediaType.parseMediaType("application/x-yaml"),
                MediaType.parseMediaType("application/yaml"),
                MediaType.parseMediaType("text/yaml")
        ) {
            @Override
            protected boolean canRead(MediaType mediaType) {
                // Only handle YAML media types explicitly
                return mediaType != null && (
                    mediaType.isCompatibleWith(MediaType.parseMediaType("application/x-yaml")) ||
                    mediaType.isCompatibleWith(MediaType.parseMediaType("application/yaml")) ||
                    mediaType.isCompatibleWith(MediaType.parseMediaType("text/yaml"))
                );
            }

            @Override
            protected boolean canWrite(MediaType mediaType) {
                // Only write YAML when explicitly requested
                return mediaType != null && (
                    mediaType.isCompatibleWith(MediaType.parseMediaType("application/x-yaml")) ||
                    mediaType.isCompatibleWith(MediaType.parseMediaType("application/yaml")) ||
                    mediaType.isCompatibleWith(MediaType.parseMediaType("text/yaml"))
                );
            }
        };
    }
}
