package com.projectArka.order_service.domain.port.out;

import com.projectArka.order_service.domain.model.OrderItem;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface IOrderItemRepository {
    Mono<OrderItem> save(OrderItem orderItem);
    Flux<OrderItem> saveAll(Iterable<OrderItem> orderItems);
    Mono<OrderItem> findById(UUID id);
    Flux<OrderItem> findByOrderId(UUID orderId);
    Mono<Void> deleteById(UUID id);
    Mono<Void> deleteByOrderId(UUID orderId);
}