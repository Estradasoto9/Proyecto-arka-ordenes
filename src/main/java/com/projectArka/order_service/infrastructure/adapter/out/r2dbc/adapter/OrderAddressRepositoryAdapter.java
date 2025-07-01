package com.projectArka.order_service.infrastructure.adapter.out.r2dbc.adapter;

import com.projectArka.order_service.domain.model.OrderAddress;
import com.projectArka.order_service.domain.port.out.IOrderAddressRepository;
import com.projectArka.order_service.infrastructure.adapter.out.r2dbc.repository.SpringDataR2bcOrderAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderAddressRepositoryAdapter implements IOrderAddressRepository {

    private final SpringDataR2bcOrderAddressRepository orderAddressRepository;

    @Override
    public Mono<OrderAddress> save(OrderAddress address) {
        return orderAddressRepository.save(address);
    }

    @Override
    public Mono<OrderAddress> findById(UUID id) {
        return orderAddressRepository.findById(id);
    }

    @Override
    public Mono<Void> deleteById(UUID id) {
        return orderAddressRepository.deleteById(id);
    }
}