package com.projectArka.order_service.application.usecase;

import com.projectArka.order_service.application.dto.OrderItemRequestDTO;
import com.projectArka.order_service.application.dto.OrderItemResponseDTO;
import com.projectArka.order_service.application.dto.OrderRequestDTO;
import com.projectArka.order_service.application.dto.OrderResponseDTO;
import com.projectArka.order_service.application.mapper.IOrderMapper;
import com.projectArka.order_service.domain.exception.OrderNotFoundException;
import com.projectArka.order_service.domain.exception.InsufficientStockException;
import com.projectArka.order_service.domain.exception.InvalidOrderDataException;
import com.projectArka.order_service.domain.exception.UserNotFoundException;
import com.projectArka.order_service.domain.model.Order;
import com.projectArka.order_service.domain.model.OrderAddress;
import com.projectArka.order_service.domain.model.OrderItem;
import com.projectArka.order_service.domain.model.Shipment;
import com.projectArka.order_service.domain.port.in.IOrderManagement;
import com.projectArka.order_service.domain.port.out.IOrderAddressRepository;
import com.projectArka.order_service.domain.port.out.IOrderItemRepository;
import com.projectArka.order_service.domain.port.out.IOrderRepository;
import com.projectArka.order_service.domain.port.out.IShipmentRepository;
import com.projectArka.order_service.infrastructure.adapter.out.r2dbc.client.IProductServiceClient;
import com.projectArka.order_service.infrastructure.adapter.out.r2dbc.client.IUserServiceClient;
import com.projectArka.order_service.infrastructure.adapter.out.r2dbc.client.ProductDetailsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderManagementUseCase implements IOrderManagement {

    private final IOrderRepository orderRepository;
    private final IOrderItemRepository orderItemRepository;
    private final IOrderAddressRepository orderAddressRepository;
    private final IShipmentRepository shipmentRepository;
    private final IUserServiceClient userServiceClient;
    private final IProductServiceClient productServiceClient;
    private final IOrderMapper orderMapper;

    @Override
    @Transactional
    public Mono<OrderResponseDTO> createOrder(OrderRequestDTO requestDTO) {
        log.info("Attempting to create order for userId: {}", requestDTO.getUserId());

        UUID userId;
        try {
            userId = UUID.fromString(requestDTO.getUserId());
        } catch (IllegalArgumentException e) {
            return Mono.error(new InvalidOrderDataException("Invalid user ID format: " + requestDTO.getUserId()));
        }

        return userServiceClient.validateUserExists(userId)
                .flatMap(userExists -> {
                    if (!userExists) {
                        return Mono.error(new UserNotFoundException("User with ID " + requestDTO.getUserId() + " not found."));
                    }
                    return Flux.fromIterable(requestDTO.getItems())
                            .flatMap(itemRequest -> {
                                UUID productId;
                                try {
                                    productId = UUID.fromString(itemRequest.getProductId());
                                } catch (IllegalArgumentException e) {
                                    return Mono.error(new InvalidOrderDataException("Invalid product ID format: " + itemRequest.getProductId()));
                                }
                                return productServiceClient.getProductDetails(productId)
                                        .switchIfEmpty(Mono.error(new InvalidOrderDataException("Product with ID " + itemRequest.getProductId() + " not found.")))
                                        .flatMap(productDetails -> productServiceClient.checkProductStock(productId, itemRequest.getQuantity())
                                                .flatMap(stockResponseMap -> {
                                                    Boolean hasStock = stockResponseMap.getOrDefault("available", false);
                                                    if (!hasStock) {
                                                        return Mono.error(new InsufficientStockException("Insufficient stock for product ID: " + itemRequest.getProductId()));
                                                    }
                                                    return Mono.just(Tuples.of(itemRequest, productDetails));
                                                })
                                        );
                            })
                            .collectList()
                            .flatMap(productTuples -> {
                                List<OrderItem> orderItems = productTuples.stream()
                                        .map(tuple -> {
                                            OrderItemRequestDTO itemRequest = tuple.getT1();
                                            ProductDetailsResponse productDetails = tuple.getT2();
                                            OrderItem item = orderMapper.toOrderItem(itemRequest);
                                            item.setUnitPrice(productDetails.getPrice());
                                            return item;
                                        })
                                        .collect(Collectors.toList());

                                BigDecimal totalAmount = orderItems.stream()
                                        .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                if (orderItems.isEmpty()) {
                                    return Mono.error(new InvalidOrderDataException("Order must contain at least one valid item."));
                                }

                                Mono<OrderAddress> savedShippingAddressMono = orderAddressRepository.save(orderMapper.toOrderAddress(requestDTO.getShippingAddress()));
                                Mono<OrderAddress> savedBillingAddressMono = orderAddressRepository.save(orderMapper.toOrderAddress(requestDTO.getBillingAddress()));

                                return Mono.zip(savedShippingAddressMono, savedBillingAddressMono)
                                        .flatMap(addressesTuple -> {
                                            OrderAddress savedShippingAddress = addressesTuple.getT1();
                                            OrderAddress savedBillingAddress = addressesTuple.getT2();

                                            Order order = Order.builder()
                                                    .userId(userId)
                                                    .orderDate(Instant.now())
                                                    .status("PENDING")
                                                    .totalAmount(totalAmount)
                                                    .shippingAddressId(savedShippingAddress.getId())
                                                    .billingAddressId(savedBillingAddress.getId())
                                                    .createdAt(Instant.now())
                                                    .updatedAt(Instant.now())
                                                    .build();

                                            return orderRepository.save(order)
                                                    .flatMap(savedOrder -> {
                                                        orderItems.forEach(item -> {
                                                            item.setOrderId(savedOrder.getId());
                                                            log.debug("OrderItem ID antes de guardar: {}", item.getId());
                                                        });
                                                        return orderItemRepository.saveAll(orderItems)
                                                                .collectList()
                                                                .flatMap(persistedOrderItems -> {
                                                                    Shipment shipment = Shipment.builder()
                                                                            .orderId(savedOrder.getId())
                                                                            .trackingNumber("TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                                                                            .status("PREPARING")
                                                                            .shippingDate(Instant.now().plusSeconds(86400 * 3))
                                                                            .build();

                                                                    return shipmentRepository.save(shipment)
                                                                            .then(Mono.just(Tuples.of(savedOrder, persistedOrderItems)));
                                                                });
                                                    })
                                                    .flatMap(orderAndItemsTuple -> {
                                                        Order savedOrder = orderAndItemsTuple.getT1();
                                                        List<OrderItem> persistedOrderItems = orderAndItemsTuple.getT2();

                                                        return Flux.fromIterable(persistedOrderItems)
                                                                .concatMap(item -> productServiceClient.decreaseProductStock(item.getProductId(), item.getQuantity()))
                                                                .then(Mono.just(savedOrder));
                                                    })
                                                    .flatMap(this::buildOrderResponseDTO);
                                        });
                            });
                });
    }

    @Override
    public Mono<OrderResponseDTO> getOrderById(String orderId) {
        log.info("Fetching order with ID: {}", orderId);
        UUID orderUuid;
        try {
            orderUuid = UUID.fromString(orderId);
        } catch (IllegalArgumentException e) {
            return Mono.error(new InvalidOrderDataException("Invalid order ID format: " + orderId));
        }
        return orderRepository.findById(orderUuid)
                .switchIfEmpty(Mono.error(new OrderNotFoundException("Order with ID " + orderId + " not found.")))
                .flatMap(this::buildOrderResponseDTO);
    }

    @Override
    public Flux<OrderResponseDTO> getOrdersByUserId(String userId) {
        log.info("Fetching orders for user ID: {}", userId);
        UUID userUuid;
        try {
            userUuid = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            return Flux.error(new InvalidOrderDataException("Invalid user ID format: " + userId));
        }
        return orderRepository.findByUserId(userUuid)
                .flatMap(this::buildOrderResponseDTO);
    }

    @Override
    public Mono<OrderResponseDTO> updateOrderStatus(String orderId, String newStatus) {
        log.info("Updating status for order ID: {} to {}", orderId, newStatus);
        UUID orderUuid;
        try {
            orderUuid = UUID.fromString(orderId);
        } catch (IllegalArgumentException e) {
            return Mono.error(new InvalidOrderDataException("Invalid order ID format: " + orderId));
        }
        return orderRepository.findById(orderUuid)
                .switchIfEmpty(Mono.error(new OrderNotFoundException("Order with ID " + orderId + " not found.")))
                .flatMap(order -> {
                    order.setStatus(newStatus);
                    order.setUpdatedAt(Instant.now());
                    return orderRepository.save(order);
                })
                .flatMap(this::buildOrderResponseDTO);
    }

    @Override
    public Mono<Void> cancelOrder(String orderId) {
        log.info("Attempting to cancel order with ID: {}", orderId);
        UUID orderUuid;
        try {
            orderUuid = UUID.fromString(orderId);
        } catch (IllegalArgumentException e) {
            return Mono.error(new InvalidOrderDataException("Invalid order ID format: " + orderId));
        }
        return orderRepository.findById(orderUuid)
                .switchIfEmpty(Mono.error(new OrderNotFoundException("Order with ID " + orderId + " not found.")))
                .flatMap(order -> {
                    if (!"CANCELLED".equals(order.getStatus()) && !"DELIVERED".equals(order.getStatus())) {
                        order.setStatus("CANCELLED");
                        order.setUpdatedAt(Instant.now());
                        return orderRepository.save(order)
                                .then(orderItemRepository.findByOrderId(order.getId())
                                        .flatMap(item -> productServiceClient.increaseProductStock(item.getProductId(), item.getQuantity()))
                                        .then()
                                );
                    } else {
                        log.warn("Order {} cannot be cancelled in status: {}", orderId, order.getStatus());
                        return Mono.empty();
                    }
                })
                .then();
    }

    @Override
    public Flux<OrderResponseDTO> getAllOrders() {
        log.info("Fetching all orders.");
        return orderRepository.findAll()
                .flatMap(this::buildOrderResponseDTO);
    }

    private Mono<OrderResponseDTO> buildOrderResponseDTO(Order order) {
        Mono<OrderAddress> shippingAddressMono = Mono.justOrEmpty(order.getShippingAddressId())
                .flatMap(orderAddressRepository::findById)
                .defaultIfEmpty(new OrderAddress());

        Mono<OrderAddress> billingAddressMono = Mono.justOrEmpty(order.getBillingAddressId())
                .flatMap(orderAddressRepository::findById)
                .defaultIfEmpty(new OrderAddress());

        Flux<OrderItem> orderItemsFlux = orderItemRepository.findByOrderId(order.getId());
        Mono<Shipment> shipmentMono = shipmentRepository.findByOrderId(order.getId())
                .defaultIfEmpty(new Shipment());

        return Mono.zip(shippingAddressMono, billingAddressMono, orderItemsFlux.collectList(), shipmentMono)
                .flatMap(tuple -> {
                    OrderAddress shippingAddress = tuple.getT1();
                    OrderAddress billingAddress = tuple.getT2();
                    List<OrderItem> items = tuple.getT3();
                    Shipment shipment = tuple.getT4();

                    OrderResponseDTO responseDTO = orderMapper.toOrderResponseDTO(order);
                    responseDTO.setShippingAddress(orderMapper.toOrderAddressDTO(shippingAddress));
                    responseDTO.setBillingAddress(orderMapper.toOrderAddressDTO(billingAddress));
                    responseDTO.setShipment(orderMapper.toShipmentDTO(shipment));

                    return Flux.fromIterable(items)
                            .flatMap(item -> productServiceClient.getProductDetails(item.getProductId())
                                    .map(productDetails -> {
                                        OrderItemResponseDTO itemDTO = orderMapper.toOrderItemResponseDTO(item);
                                        itemDTO.setProductName(productDetails.getName());
                                        return itemDTO;
                                    })
                                    .onErrorResume(e -> {
                                        log.error("Could not fetch product details for item {}: {}", item.getProductId(), e.getMessage());
                                        OrderItemResponseDTO itemDTO = orderMapper.toOrderItemResponseDTO(item);
                                        itemDTO.setProductName("Product Name Unavailable");
                                        return Mono.just(itemDTO);
                                    })
                            )
                            .collectList()
                            .doOnNext(responseDTO::setItems)
                            .thenReturn(responseDTO);
                });
    }
}