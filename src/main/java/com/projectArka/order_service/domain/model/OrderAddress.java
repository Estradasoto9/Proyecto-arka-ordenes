package com.projectArka.order_service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("order_address")
public class OrderAddress {

    @Id
    @Column("id")
    private UUID id;
    private String street;
    private String number;
    private String apartment;
    private String city;
    private String state;
    private String country;

    @Column("postal_code")
    private String postalCode;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant  updatedAt;
}