package com.projectArka.order_service.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@Configuration
@EnableR2dbcRepositories(basePackages = "com.projectArka.order_service.infrastructure.adapter.out.r2dbc.repository")
public class R2dbcConfig {
}