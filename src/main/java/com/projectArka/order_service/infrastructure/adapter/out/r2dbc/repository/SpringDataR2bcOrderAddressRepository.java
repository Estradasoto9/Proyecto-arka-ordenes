package com.projectArka.order_service.infrastructure.adapter.out.r2dbc.repository;

import com.projectArka.order_service.domain.model.OrderAddress;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SpringDataR2bcOrderAddressRepository extends R2dbcRepository<OrderAddress, UUID> {
}