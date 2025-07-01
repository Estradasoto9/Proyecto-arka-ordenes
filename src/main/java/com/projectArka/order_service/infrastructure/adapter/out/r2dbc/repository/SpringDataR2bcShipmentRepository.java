package com.projectArka.order_service.infrastructure.adapter.out.r2dbc.repository;

import com.projectArka.order_service.domain.model.Shipment;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SpringDataR2bcShipmentRepository extends R2dbcRepository<Shipment, UUID> {
    Mono<Shipment> findByOrderId(UUID orderId);
    Mono<Void> deleteByOrderId(UUID orderId);
}