package com.projectArka.order_service.infrastructure.adapter.out.r2dbc.client;

import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

public interface IProductServiceClient {
    Mono<ProductDetailsResponse> getProductDetails(UUID productId);
    Mono<Map<String, Boolean>> checkProductStock(UUID productId, int quantity);
    Mono<Void> decreaseProductStock(UUID productId, int quantity);
    Mono<Void> increaseProductStock(UUID productId, int quantity);
}