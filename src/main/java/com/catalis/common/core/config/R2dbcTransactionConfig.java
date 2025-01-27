package com.catalis.common.core.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Configuration class for managing R2DBC transactions in a reactive application.
 *
 * This class enables Spring's reactive transaction management and defines a bean for
 * the {@link ReactiveTransactionManager} configured with an R2DBC {@link ConnectionFactory}.
 *
 * Key features:
 * - Provides reactive transaction manager support using {@link R2dbcTransactionManager}.
 * - Makes transaction management available for reactive database operations within the application.
 *
 * Annotations:
 * - {@code @Configuration}: Indicates that this class is a Spring configuration class.
 * - {@code @EnableTransactionManagement}: Enables Spring's annotation-driven transaction management capabilities.
 *
 * Bean Definitions:
 * - {@link ReactiveTransactionManager}: Configured to manage transactions for R2DBC-based reactive data sources.
 */
@Configuration
@EnableTransactionManagement
public class R2dbcTransactionConfig {

    @Bean
    public ReactiveTransactionManager transactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }

}