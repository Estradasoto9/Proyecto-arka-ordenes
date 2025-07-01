package com.projectArka.order_service.infrastructure.adapter.out.r2dbc.client.impl;

import com.projectArka.order_service.infrastructure.adapter.out.r2dbc.client.IProductServiceClient;
import com.projectArka.order_service.infrastructure.adapter.out.r2dbc.client.ProductDetailsResponse;
import com.projectArka.order_service.infrastructure.config.WebClientConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class ProductServiceClientImpl implements IProductServiceClient {

    private final WebClient productWebClient;
    private final String productDetailPath;
    private final String productStockCheckPath;
    private final String productStockDecreasePath;
    private final String productStockIncreasePath;

    public ProductServiceClientImpl(
            WebClientConfig webClientConfig,
            @Value("${clients.product-service.url}") String productBaseUrl,
            @Value("${clients.product-service.paths.details}") String productDetailPath,
            @Value("${clients.product-service.paths.stock-check}") String productStockCheckPath,
            @Value("${clients.product-service.paths.stock-decrease}") String productStockDecreasePath,
            @Value("${clients.product-service.paths.stock-increase}") String productStockIncreasePath) {
        this.productWebClient = webClientConfig.productWebClient(productBaseUrl);
        this.productDetailPath = productDetailPath;
        this.productStockCheckPath = productStockCheckPath;
        this.productStockDecreasePath = productStockDecreasePath;
        this.productStockIncreasePath = productStockIncreasePath;
    }

    @Override
    public Mono<ProductDetailsResponse> getProductDetails(UUID productId) {
        String productIdString = productId.toString();
        log.info("Calling Product Service for details of product ID: {}", productIdString);
        return productWebClient.get()
                .uri(productDetailPath, productIdString)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    return response.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                log.error("Client Error fetching product details for ID {}: {} - {}", productIdString, response.statusCode(), errorBody);
                                if (response.statusCode() == HttpStatus.NOT_FOUND) {
                                    return Mono.error(new RuntimeException("Product not found: " + productIdString));
                                }
                                return Mono.error(new RuntimeException("Product service client error: " + response.statusCode() + " - " + errorBody));
                            });
                })
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        response.bodyToMono(String.class).flatMap(errorBody -> {
                            log.error("Server Error fetching product details for ID {}: {} - {}", productIdString, response.statusCode(), errorBody);
                            return Mono.error(new RuntimeException("Product service server error: " + response.statusCode() + " - " + errorBody));
                        })
                )
                .bodyToMono(ProductDetailsResponse.class)
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2))
                        .filter(throwable ->
                                throwable instanceof WebClientRequestException ||
                                        (throwable instanceof RuntimeException && throwable.getMessage().contains("Service Unavailable"))
                        )
                        .doBeforeRetry(retrySignal -> log.warn("Retrying getProductDetails for ID {} (attempt {}), due to: {}",
                                productIdString, retrySignal.totalRetriesInARow() + 1, retrySignal.failure().getMessage()))
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new RuntimeException("Product service details call exhausted retries after " + retrySignal.totalRetriesInARow() + " attempts", retrySignal.failure())));
    }

    @Override
    public Mono<Map<String, Boolean>> checkProductStock(UUID productId, int quantity) {
        String productIdString = productId.toString();
        log.info("Calling Product Service to check stock for product ID: {} (quantity: {})", productIdString, quantity);
        return productWebClient.get()
                .uri(productStockCheckPath, productIdString, quantity)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Error checking product stock for ID {}: {} - {}", productIdString, response.statusCode(), errorBody);
                                    if (response.statusCode() == HttpStatus.NOT_FOUND) {
                                        return Mono.empty();
                                    }
                                    return Mono.error(new RuntimeException("Product service error checking stock: " + response.statusCode() + " - " + errorBody));
                                })
                )
                .bodyToMono(new ParameterizedTypeReference<Map<String, Boolean>>() {})
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Product ID {} not found during stock check or client error. Returning available: false.", productIdString);
                    return Mono.just(Map.of("available", false));
                }))
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2))
                        .filter(throwable ->
                                throwable instanceof WebClientRequestException ||
                                        (throwable instanceof RuntimeException && throwable.getMessage().contains("Service Unavailable"))
                        )
                        .doBeforeRetry(retrySignal -> log.warn("Retrying checkProductStock for ID {} (attempt {}), due to: {}",
                                productIdString, retrySignal.totalRetriesInARow() + 1, retrySignal.failure().getMessage()))
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new RuntimeException("Product service stock check call exhausted retries after " + retrySignal.totalRetriesInARow() + " attempts", retrySignal.failure())));
    }

    @Override
    public Mono<Void> decreaseProductStock(UUID productId, int quantity) {
        String productIdString = productId.toString();
        log.info("Calling Product Service to decrease stock for product ID: {} (quantity: {})", productIdString, quantity);
        return productWebClient.put()
                .uri(productStockDecreasePath, productIdString)
                .bodyValue(Map.of("quantity", quantity))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    return response.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                log.error("Client Error decreasing product stock for ID {}: {} - {}", productIdString, response.statusCode(), errorBody);
                                if (response.statusCode() == HttpStatus.BAD_REQUEST) {
                                    return Mono.error(new RuntimeException("Bad request for stock decrease: " + productIdString + " - " + errorBody));
                                }
                                if (response.statusCode() == HttpStatus.NOT_FOUND) {
                                    return Mono.error(new RuntimeException("Product not found for stock decrease: " + productIdString));
                                }
                                return Mono.error(new RuntimeException("Product service client error: " + response.statusCode() + " - " + errorBody));
                            });
                })
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        response.bodyToMono(String.class).flatMap(errorBody -> {
                            log.error("Server Error decreasing product stock for ID {}: {} - {}", productIdString, response.statusCode(), errorBody);
                            return Mono.error(new RuntimeException("Product service server error: " + response.statusCode() + " - " + errorBody));
                        })
                )
                .toBodilessEntity()
                .then()
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2))
                        .filter(throwable ->
                                throwable instanceof WebClientRequestException ||
                                        (throwable instanceof RuntimeException && throwable.getMessage().contains("Service Unavailable"))
                        )
                        .doBeforeRetry(retrySignal -> log.warn("Retrying decreaseProductStock for ID {} (attempt {}), due to: {}",
                                productIdString, retrySignal.totalRetriesInARow() + 1, retrySignal.failure().getMessage()))
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new RuntimeException("Product service stock decrease call exhausted retries after " + retrySignal.totalRetriesInARow() + " attempts", retrySignal.failure())));
    }

    @Override
    public Mono<Void> increaseProductStock(UUID productId, int quantity) {
        String productIdString = productId.toString();
        log.info("Calling Product Service to increase stock for product ID: {} (quantity: {})", productIdString, quantity);
        return productWebClient.put()
                .uri(productStockIncreasePath, productIdString)
                .bodyValue(Map.of("quantity", quantity))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    return response.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                log.error("Client Error increasing product stock for ID {}: {} - {}", productIdString, response.statusCode(), errorBody);
                                if (response.statusCode() == HttpStatus.BAD_REQUEST) {
                                    return Mono.error(new RuntimeException("Bad request for stock increase: " + productIdString + " - " + errorBody));
                                }
                                if (response.statusCode() == HttpStatus.NOT_FOUND) {
                                    return Mono.error(new RuntimeException("Product not found for stock increase: " + productIdString));
                                }
                                return Mono.error(new RuntimeException("Product service client error: " + response.statusCode() + " - " + errorBody));
                            });
                })
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        response.bodyToMono(String.class).flatMap(errorBody -> {
                            log.error("Server Error increasing product stock for ID {}: {} - {}", productIdString, response.statusCode(), errorBody);
                            return Mono.error(new RuntimeException("Product service server error: " + response.statusCode() + " - " + errorBody));
                        })
                )
                .toBodilessEntity()
                .then()
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2))
                        .filter(throwable ->
                                throwable instanceof WebClientRequestException ||
                                        (throwable instanceof RuntimeException && throwable.getMessage().contains("Service Unavailable"))
                        )
                        .doBeforeRetry(retrySignal -> log.warn("Retrying increaseProductStock for ID {} (attempt {}), due to: {}",
                                productIdString, retrySignal.totalRetriesInARow() + 1, retrySignal.failure().getMessage()))
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new RuntimeException("Product service stock increase call exhausted retries after " + retrySignal.totalRetriesInARow() + " attempts", retrySignal.failure())));
    }
}