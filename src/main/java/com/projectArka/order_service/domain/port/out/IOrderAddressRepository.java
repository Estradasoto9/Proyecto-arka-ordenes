package com.projectArka.order_service.domain.port.out;

import com.projectArka.order_service.domain.model.OrderAddress;
import reactor.core.publisher.Mono;

import java.util.UUID;


public interface IOrderAddressRepository {
    Mono<OrderAddress> save(OrderAddress address);
    Mono<OrderAddress> findById(UUID id);
    Mono<Void> deleteById(UUID id);
}