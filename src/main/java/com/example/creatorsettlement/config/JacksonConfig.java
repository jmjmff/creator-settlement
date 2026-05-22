package com.example.creatorsettlement.config;

import com.fasterxml.jackson.core.JsonGenerator;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer disableNonAsciiEscape() {
        return builder -> builder.featuresToDisable(JsonGenerator.Feature.ESCAPE_NON_ASCII);
    }
}
