package com.projectArka.order_service.infrastructure.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("orders")
public class Order {
    @Id
    @Column("id")
    private UUID id;

    @Column("order_date")
    private Instant orderDate;

    private String status;

    @Column("total_amount")
    private BigDecimal totalAmount;

    @Column("shipping_address_id")
    private UUID shippingAddressId;

    @Column("billing_address_id")
    private UUID billingAddressId;

    @Column("user_id")
    private UUID userId;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;
}