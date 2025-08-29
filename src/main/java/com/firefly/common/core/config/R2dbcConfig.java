package com.firefly.common.core.config;

import com.firefly.common.core.filters.FilterUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;

@Configuration
public class R2dbcConfig {
    private final R2dbcEntityTemplate template;

    public R2dbcConfig(R2dbcEntityTemplate template) {
        this.template = template;
    }

    @PostConstruct
    public void initialize() {
        FilterUtils.initializeTemplate(template);
    }
}