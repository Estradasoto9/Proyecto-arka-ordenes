package com.projectArka.order_service.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequestDTO {
    @NotBlank(message = "User ID cannot be blank")
    private String userId;

    @NotNull(message = "Shipping address is required")
    @Valid
    private OrderAddressDTO shippingAddress;

    @NotNull(message = "Billing address is required")
    @Valid
    private OrderAddressDTO billingAddress;

    @NotNull(message = "Order items cannot be null")
    @Valid
    private List<OrderItemRequestDTO> items;
}