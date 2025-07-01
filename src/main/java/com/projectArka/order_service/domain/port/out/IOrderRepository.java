package com.projectArka.order_service.domain.port.out;

import com.projectArka.order_service.domain.model.Order;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface IOrderRepository {
    Mono<Order> save(Order order);
    Mono<Order> findById(UUID id);
    Flux<Order> findAll();
    Flux<Order> findByUserId(UUID userId);
    Mono<Void> deleteById(UUID id);
}