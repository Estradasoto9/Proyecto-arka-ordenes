package com.projectArka.order_service.infrastructure.config;

import com.projectArka.order_service.domain.exception.OrderNotFoundException;
import com.projectArka.order_service.domain.exception.UserNotFoundException;
import com.projectArka.order_service.domain.exception.InvalidOrderDataException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<String> handleOrderNotFoundException(OrderNotFoundException ex) {
        return Mono.just(ex.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<String> handleUserNotFoundException(UserNotFoundException ex) {
        return Mono.just(ex.getMessage());
    }

    @ExceptionHandler(InvalidOrderDataException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<String> handleInvalidOrderDataException(InvalidOrderDataException ex) {
        return Mono.just(ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<String> handleRuntimeException(RuntimeException ex) {
        return Mono.just("An unexpected error occurred: " + ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<String> handleGenericException(Exception ex) {
        return Mono.just("An internal server error occurred: " + ex.getMessage());
    }
}