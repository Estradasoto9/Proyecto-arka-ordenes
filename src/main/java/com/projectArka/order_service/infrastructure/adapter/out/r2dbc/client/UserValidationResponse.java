package com.projectArka.order_service.infrastructure.adapter.out.r2dbc.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserValidationResponse {
    private UUID userId;
    private String username;
    private String email;
}