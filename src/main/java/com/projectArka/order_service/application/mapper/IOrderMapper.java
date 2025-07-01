package com.projectArka.order_service.application.mapper;

import com.projectArka.order_service.application.dto.OrderAddressDTO;
import com.projectArka.order_service.application.dto.OrderItemRequestDTO;
import com.projectArka.order_service.application.dto.OrderItemResponseDTO;
import com.projectArka.order_service.application.dto.OrderRequestDTO;
import com.projectArka.order_service.application.dto.OrderResponseDTO;
import com.projectArka.order_service.application.dto.ShipmentDTO;
import com.projectArka.order_service.domain.model.Order;
import com.projectArka.order_service.domain.model.OrderAddress;
import com.projectArka.order_service.domain.model.OrderItem;
import com.projectArka.order_service.domain.model.Shipment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface IOrderMapper {

    IOrderMapper INSTANCE = Mappers.getMapper(IOrderMapper.class);

    @Named("uuidToString")
    default String uuidToString(UUID uuid) {
        return uuid != null ? uuid.toString() : null;
    }

    @Named("stringToUuid")
    default UUID stringToUuid(String id) {
        return id != null ? UUID.fromString(id) : null;
    }

    @Named("instantToLocalDateTime")
    default LocalDateTime instantToLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneOffset.UTC) : null;
    }

    @Named("localDateTimeToInstant")
    default Instant localDateTimeToInstant(LocalDateTime localDateTime) {
        return localDateTime != null ? localDateTime.toInstant(ZoneOffset.UTC) : null;
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    OrderAddress toOrderAddress(OrderAddressDTO orderAddressDTO);

    @Mapping(source = "id", target = "id", qualifiedByName = "uuidToString")
    @Mapping(source = "createdAt", target = "createdAt", qualifiedByName = "instantToLocalDateTime")
    @Mapping(source = "updatedAt", target = "updatedAt", qualifiedByName = "instantToLocalDateTime")
    OrderAddressDTO toOrderAddressDTO(OrderAddress orderAddress);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderId", ignore = true)
    @Mapping(target = "unitPrice", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(source = "productId", target = "productId", qualifiedByName = "stringToUuid")
    OrderItem toOrderItem(OrderItemRequestDTO orderItemRequestDTO);

    @Mapping(source = "productId", target = "productId", qualifiedByName = "uuidToString")
    @Mapping(target = "productName", ignore = true)
    OrderItemResponseDTO toOrderItemResponseDTO(OrderItem orderItem);

    @Mapping(source = "id", target = "idShipment", qualifiedByName = "uuidToString")
    @Mapping(source = "orderId", target = "orderId", qualifiedByName = "uuidToString")
    @Mapping(source = "shippingDate", target = "shippingDate", qualifiedByName = "instantToLocalDateTime")
    @Mapping(source = "createdAt", target = "createdAt", qualifiedByName = "instantToLocalDateTime")
    @Mapping(source = "updatedAt", target = "updatedAt", qualifiedByName = "instantToLocalDateTime")
    ShipmentDTO toShipmentDTO(Shipment shipment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderDate", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "shippingAddressId", ignore = true)
    @Mapping(target = "billingAddressId", ignore = true)
    @Mapping(source = "userId", target = "userId", qualifiedByName = "stringToUuid")
    Order toOrder(OrderRequestDTO orderRequestDTO);

    @Mapping(source = "id", target = "orderId", qualifiedByName = "uuidToString")
    @Mapping(source = "userId", target = "userId", qualifiedByName = "uuidToString")
    @Mapping(target = "shippingAddress", ignore = true)
    @Mapping(target = "billingAddress", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "shipment", ignore = true)
    @Mapping(source = "orderDate", target = "orderDate", qualifiedByName = "instantToLocalDateTime")
    @Mapping(source = "createdAt", target = "createdAt", qualifiedByName = "instantToLocalDateTime")
    @Mapping(source = "updatedAt", target = "updatedAt", qualifiedByName = "instantToLocalDateTime")
    OrderResponseDTO toOrderResponseDTO(Order order);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderId", ignore = true)
    @Mapping(target = "unitPrice", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(source = "productId", target = "productId", qualifiedByName = "stringToUuid")
    List<OrderItem> toOrderItems(List<OrderItemRequestDTO> items);

    @Mapping(source = "productId", target = "productId", qualifiedByName = "uuidToString")
    @Mapping(target = "productName", ignore = true)
    List<OrderItemResponseDTO> toOrderItemResponseDTOs(List<OrderItem> items);
}