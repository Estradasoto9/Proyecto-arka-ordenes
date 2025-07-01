package com.projectArka.order_service.infrastructure.adapter.out.r2dbc.repository;

import com.projectArka.order_service.domain.model.OrderItem;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SpringDataR2bcOrderItemRepository extends R2dbcRepository<OrderItem, UUID> {
    Flux<OrderItem> findByOrderId(UUID orderId);
    Mono<Void> deleteByOrderId(UUID orderId);
}