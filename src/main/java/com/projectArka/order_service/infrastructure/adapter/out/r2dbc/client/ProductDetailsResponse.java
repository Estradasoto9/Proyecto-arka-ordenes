package com.projectArka.order_service.infrastructure.adapter.out.r2dbc.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDetailsResponse {
    private UUID productId;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;

}