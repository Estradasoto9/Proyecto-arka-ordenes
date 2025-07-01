package com.projectArka.order_service.infrastructure.adapter.out.r2dbc.adapter;

import com.projectArka.order_service.domain.model.Shipment;
import com.projectArka.order_service.domain.port.out.IShipmentRepository;
import com.projectArka.order_service.infrastructure.adapter.out.r2dbc.repository.SpringDataR2bcShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ShipmentRepositoryAdapter implements IShipmentRepository {

    private final SpringDataR2bcShipmentRepository shipmentRepository;

    @Override
    public Mono<Shipment> save(Shipment shipment) {
        return shipmentRepository.save(shipment);
    }

    @Override
    public Mono<Shipment> findById(UUID id) {
        return shipmentRepository.findById(id);
    }

    @Override
    public Mono<Shipment> findByOrderId(UUID orderId) {
        return shipmentRepository.findByOrderId(orderId);
    }

    @Override
    public Mono<Void> deleteById(UUID id) {
        return shipmentRepository.deleteById(id);
    }

    @Override
    public Mono<Void> deleteByOrderId(UUID orderId) {
        return shipmentRepository.deleteByOrderId(orderId);
    }
}