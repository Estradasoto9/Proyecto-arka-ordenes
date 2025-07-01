package com.projectArka.order_service.domain.model;

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
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("order_item")
public class OrderItem {
    @Id
    private UUID id;

    private Integer quantity;

    @Column("unit_price")
    private BigDecimal unitPrice;

    @Column("order_id")
    private UUID orderId;

    @Column("product_id")
    private UUID productId;

    @CreatedDate
    @Column("created_at")
    private Instant  createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;
}