package com.projectArka.order_service.infrastructure.adapter.out.r2dbc.adapter;

import com.projectArka.order_service.domain.model.OrderItem;
import com.projectArka.order_service.domain.port.out.IOrderItemRepository;
import com.projectArka.order_service.infrastructure.adapter.out.r2dbc.repository.SpringDataR2bcOrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderItemRepositoryAdapter implements IOrderItemRepository {

    private final SpringDataR2bcOrderItemRepository orderItemRepository;

    @Override
    public Mono<OrderItem> save(OrderItem orderItem) {
        return orderItemRepository.save(orderItem);
    }

    @Override
    public Flux<OrderItem> saveAll(Iterable<OrderItem> orderItems) {
        return orderItemRepository.saveAll(orderItems);
    }

    @Override
    public Mono<OrderItem> findById(UUID id) {
        return orderItemRepository.findById(id);
    }

    @Override
    public Flux<OrderItem> findByOrderId(UUID orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    @Override
    public Mono<Void> deleteById(UUID id) {
        return orderItemRepository.deleteById(id);
    }

    @Override
    public Mono<Void> deleteByOrderId(UUID orderId) {
        return orderItemRepository.deleteByOrderId(orderId);
    }
}