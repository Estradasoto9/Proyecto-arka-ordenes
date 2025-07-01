package com.projectArka.order_service.domain.port.in;

import com.projectArka.order_service.application.dto.OrderRequestDTO;
import com.projectArka.order_service.application.dto.OrderResponseDTO;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

public interface IOrderManagement {
    Mono<OrderResponseDTO> createOrder(OrderRequestDTO requestDTO);
    Mono<OrderResponseDTO> getOrderById(String orderId);
    Flux<OrderResponseDTO> getOrdersByUserId(String userId);
    Mono<OrderResponseDTO> updateOrderStatus(String orderId, String newStatus);
    Mono<Void> cancelOrder(String orderId);
    Flux<OrderResponseDTO> getAllOrders();
}