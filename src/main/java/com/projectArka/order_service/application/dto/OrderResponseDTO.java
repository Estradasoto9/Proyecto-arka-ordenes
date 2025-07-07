package com.projectArka.order_service.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class OrderResponseDTO {
    private String orderId;
    private LocalDateTime orderDate;
    private String status;
    private BigDecimal totalAmount;
    private String userId;
    private OrderAddressDTO shippingAddress;
    private OrderAddressDTO billingAddress;
    private List<OrderItemResponseDTO> items;
    private ShipmentDTO shipment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}