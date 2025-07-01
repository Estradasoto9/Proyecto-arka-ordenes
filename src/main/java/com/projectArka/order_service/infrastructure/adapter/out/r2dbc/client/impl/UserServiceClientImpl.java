package com.projectArka.order_service.infrastructure.adapter.out.r2dbc.client.impl;

import com.projectArka.order_service.infrastructure.adapter.out.r2dbc.client.IUserServiceClient;
import com.projectArka.order_service.infrastructure.adapter.out.r2dbc.client.UserDetailsResponse;
import com.projectArka.order_service.infrastructure.config.WebClientConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.UUID;

@Component
@Slf4j
public class UserServiceClientImpl implements IUserServiceClient {

    private final WebClient userWebClient;
    private final String userValidationPath;
    private final String userDetailsPath;

    @Value("${application.security.jwt.test-token:}")
    private String jwtToken;

    public UserServiceClientImpl(
            WebClientConfig webClientConfig,
            @Value("${clients.user-service.url}") String userBaseUrl,
            @Value("${clients.user-service.paths.validate-user}") String userValidationPath,
            @Value("${clients.user-service.paths.details}") String userDetailsPath) {
        this.userWebClient = webClientConfig.userWebClient(userBaseUrl);
        this.userValidationPath = userValidationPath;
        this.userDetailsPath = userDetailsPath;
        log.info("UserServiceClient initialized with base URL: {}", userBaseUrl);
    }

    @Override
    public Mono<Boolean> validateUserExists(UUID userId) {
        String userIdString = userId.toString();
        log.debug("Calling User Service to validate user existence for ID: {}", userIdString);
        return userWebClient.get()
                .uri(userValidationPath, userIdString)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .retrieve()
                .onStatus(HttpStatus.NOT_FOUND::equals, clientResponse -> {
                    log.warn("User ID {} not found during validation (404 from user service).", userIdString);
                    return Mono.error(new RuntimeException("User not found: " + userIdString));
                })
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response ->
                                response.bodyToMono(String.class)
                                        .flatMap(errorBody -> {
                                            log.error("Error validating user existence for ID {}. Status: {}. Body: {}", userIdString, response.statusCode(), errorBody);
                                            return Mono.error(new RuntimeException(
                                                    "User service error during validation for ID " + userIdString + ": " + response.statusCode() + " - " + errorBody
                                            ));
                                        })
                )
                .bodyToMono(Boolean.class)
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2))
                        .filter(throwable -> throwable instanceof RuntimeException && throwable.getMessage().contains("User service error during validation"))
                        .doBeforeRetry(retrySignal -> log.warn("Retrying validateUserExists for user ID: {} (attempt {}), due to: {}",
                                userIdString, retrySignal.totalRetries() + 1, retrySignal.failure().getMessage())))
                .onErrorResume(e -> {
                    log.error("Final error validating user existence for ID: {}. Error: {}", userIdString, e.getMessage());
                    return Mono.just(false);
                });
    }

    @Override
    public Mono<UserDetailsResponse> getUserDetails(UUID userId) {
        String userIdString = userId.toString();
        log.debug("Calling User Service to get details for user ID: {}", userIdString);
        return userWebClient.get()
                .uri(userDetailsPath, userIdString)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .retrieve()
                .onStatus(HttpStatus.NOT_FOUND::equals, clientResponse -> {
                    log.warn("User ID {} not found for details (404 from user service).", userIdString);
                    return Mono.error(new RuntimeException("User not found for details: " + userIdString));
                })
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response ->
                                response.bodyToMono(String.class)
                                        .flatMap(errorBody -> {
                                            log.error("Error fetching user details for ID {}. Status: {}. Body: {}", userIdString, response.statusCode(), errorBody);
                                            return Mono.error(new RuntimeException(
                                                    "User service error during details fetch for ID " + userIdString + ": " + response.statusCode() + " - " + errorBody
                                            ));
                                        })
                )
                .bodyToMono(UserDetailsResponse.class)
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2))
                        .filter(throwable -> throwable instanceof Exception)
                        .doBeforeRetry(retrySignal -> log.warn("Retrying getUserDetails for user ID: {} (attempt {}), due to: {}",
                                userIdString, retrySignal.totalRetries() + 1, retrySignal.failure().getMessage())))
                .onErrorResume(e -> {
                    log.error("Final error fetching user details for ID: {}. Error: {}", userIdString, e.getMessage());
                    return Mono.empty();
                });
    }
}