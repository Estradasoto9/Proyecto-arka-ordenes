package com.projectArka.order_service.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentDTO {
    private String idShipment;
    private String orderId;
    private LocalDateTime shippingDate;
    private String trackingNumber;
    private String carrier;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}