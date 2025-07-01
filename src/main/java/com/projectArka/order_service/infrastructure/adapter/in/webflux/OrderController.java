package com.projectArka.order_service.infrastructure.adapter.in.webflux;

import com.projectArka.order_service.application.dto.OrderRequestDTO;
import com.projectArka.order_service.application.dto.OrderResponseDTO;
import com.projectArka.order_service.application.usecase.OrderManagementUseCase;
import com.projectArka.order_service.domain.exception.InsufficientStockException;
import com.projectArka.order_service.domain.exception.InvalidOrderDataException;
import com.projectArka.order_service.domain.exception.OrderNotFoundException;
import com.projectArka.order_service.domain.exception.UserNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderManagementUseCase orderManagementUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<OrderResponseDTO> createOrder(@Valid @RequestBody OrderRequestDTO requestDTO) {
        log.info("Received request to create order for userId: {}", requestDTO.getUserId());
        return orderManagementUseCase.createOrder(requestDTO)
                .doOnSuccess(order -> log.info("Order created successfully with ID: {}", order.getOrderId()))
                .onErrorResume(UserNotFoundException.class, e -> {
                    log.error("User not found during order creation: {}", e.getMessage());
                    return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage()));
                })
                .onErrorResume(InsufficientStockException.class, e -> {
                    log.error("Insufficient stock during order creation: {}", e.getMessage());
                    return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage()));
                })
                .onErrorResume(InvalidOrderDataException.class, e -> {
                    log.error("Invalid order data during order creation: {}", e.getMessage());
                    return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage()));
                })
                .onErrorResume(e -> {
                    log.error("Unexpected error during order creation: {}", e.getMessage(), e);
                    return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred."));
                });
    }

    @GetMapping("/{orderId}")
    public Mono<ResponseEntity<OrderResponseDTO>> getOrderById(@PathVariable String orderId) {
        log.info("Received request to get order by ID: {}", orderId);
        return orderManagementUseCase.getOrderById(orderId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .onErrorResume(OrderNotFoundException.class, e -> {
                    log.error("Order not found: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
                })
                .onErrorResume(e -> {
                    log.error("Unexpected error fetching order by ID {}: {}", orderId, e.getMessage(), e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @GetMapping("/user/{userId}")
    public Flux<OrderResponseDTO> getOrdersByUserId(@PathVariable String userId) {
        log.info("Received request to get orders for user ID: {}", userId);
        return orderManagementUseCase.getOrdersByUserId(userId)
                .doOnError(e -> log.error("Error fetching orders for user ID {}: {}", userId, e.getMessage(), e));
    }

    @PutMapping("/{orderId}/status")
    public Mono<ResponseEntity<OrderResponseDTO>> updateOrderStatus(@PathVariable String orderId, @RequestParam String newStatus) {
        log.info("Received request to update status for order ID: {} to {}", orderId, newStatus);
        return orderManagementUseCase.updateOrderStatus(orderId, newStatus)
                .map(ResponseEntity::ok)
                .onErrorResume(OrderNotFoundException.class, e -> {
                    log.error("Order not found for status update: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
                })
                .onErrorResume(e -> {
                    log.error("Unexpected error updating order status for ID {}: {}", orderId, e.getMessage(), e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @DeleteMapping("/{orderId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> cancelOrder(@PathVariable String orderId) {
        log.info("Received request to cancel order with ID: {}", orderId);
        return orderManagementUseCase.cancelOrder(orderId)
                .doOnSuccess(v -> log.info("Order {} cancelled successfully.", orderId))
                .onErrorResume(OrderNotFoundException.class, e -> {
                    log.error("Order not found for cancellation: {}", e.getMessage());
                    return Mono.error(new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage()));
                })
                .onErrorResume(e -> {
                    log.error("Unexpected error during order cancellation for ID {}: {}", orderId, e.getMessage(), e);
                    return Mono.error(new org.springframework.web.server.ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred."));
                });
    }

    @GetMapping
    public Flux<OrderResponseDTO> getAllOrders() {
        log.info("Received request to get all orders.");
        return orderManagementUseCase.getAllOrders()
                .doOnError(e -> log.error("Error fetching all orders: {}", e.getMessage(), e));
    }
}