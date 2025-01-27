package com.catalis.common.core.config;

import com.catalis.common.core.filters.FilterParameterCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public FilterParameterCustomizer filterParameterCustomizer() {
        return new FilterParameterCustomizer();
    }
}