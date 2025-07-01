package com.projectArka.order_service.infrastructure.adapter.out.r2dbc.repository;

import com.projectArka.order_service.domain.model.Order;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import org.springframework.stereotype.Repository;

import java.util.UUID;


@Repository
public interface SpringDataR2bcOrderRepository extends R2dbcRepository<Order, UUID> {
    Flux<Order> findByUserId(UUID userId);
}