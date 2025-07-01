package com.projectArka.order_service.infrastructure.adapter.out.r2dbc.adapter;

import com.projectArka.order_service.domain.model.Order;
import com.projectArka.order_service.domain.port.out.IOrderRepository;
import com.projectArka.order_service.infrastructure.adapter.out.r2dbc.repository.SpringDataR2bcOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements IOrderRepository {

    private final SpringDataR2bcOrderRepository orderRepository;

    @Override
    public Mono<Order> save(Order order) {
        return orderRepository.save(order);
    }

    @Override
    public Mono<Order> findById(UUID id) {
        return orderRepository.findById(id);
    }

    @Override
    public Flux<Order> findAll() {
        return orderRepository.findAll();
    }

    @Override
    public Flux<Order> findByUserId(UUID userId) {
        return orderRepository.findByUserId(userId);
    }

    @Override
    public Mono<Void> deleteById(UUID id) {
        return orderRepository.deleteById(id);
    }
}