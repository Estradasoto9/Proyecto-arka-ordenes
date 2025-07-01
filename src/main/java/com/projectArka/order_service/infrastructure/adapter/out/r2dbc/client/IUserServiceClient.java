package com.projectArka.order_service.infrastructure.adapter.out.r2dbc.client;

import reactor.core.publisher.Mono;

import java.util.UUID;

public interface IUserServiceClient {
    Mono<Boolean> validateUserExists(UUID userId);
    Mono<UserDetailsResponse> getUserDetails(UUID userId);
}