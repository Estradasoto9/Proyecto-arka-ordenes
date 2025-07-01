package com.projectArka.order_service.domain.port.out;

import com.projectArka.order_service.domain.model.Shipment;
import reactor.core.publisher.Mono;

import java.util.UUID;


public interface IShipmentRepository {
    Mono<Shipment> save(Shipment shipment);
    Mono<Shipment> findById(UUID id);
    Mono<Shipment> findByOrderId(UUID orderId);
    Mono<Void> deleteById(UUID id);
    Mono<Void> deleteByOrderId(UUID orderId);
}