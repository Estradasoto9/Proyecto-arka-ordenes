package com.projectArka.order_service.UsecaseTest;

import com.projectArka.order_service.application.dto.OrderAddressDTO;
import com.projectArka.order_service.application.dto.OrderItemRequestDTO;
import com.projectArka.order_service.application.dto.OrderItemResponseDTO;
import com.projectArka.order_service.application.dto.OrderRequestDTO;
import com.projectArka.order_service.application.dto.OrderResponseDTO;
import com.projectArka.order_service.application.dto.ShipmentDTO;
import com.projectArka.order_service.application.mapper.IOrderMapper;
import com.projectArka.order_service.application.usecase.OrderManagementUseCase;
import com.projectArka.order_service.domain.exception.InsufficientStockException;
import com.projectArka.order_service.domain.exception.InvalidOrderDataException;
import com.projectArka.order_service.domain.exception.OrderNotFoundException;
import com.projectArka.order_service.domain.exception.UserNotFoundException;
import com.projectArka.order_service.domain.model.Order;
import com.projectArka.order_service.domain.model.OrderAddress;
import com.projectArka.order_service.domain.model.OrderItem;
import com.projectArka.order_service.domain.model.Shipment;
import com.projectArka.order_service.domain.port.out.IOrderAddressRepository;
import com.projectArka.order_service.domain.port.out.IOrderItemRepository;
import com.projectArka.order_service.domain.port.out.IOrderRepository;
import com.projectArka.order_service.domain.port.out.IShipmentRepository;
import com.projectArka.order_service.infrastructure.adapter.out.r2dbc.client.IProductServiceClient;
import com.projectArka.order_service.infrastructure.adapter.out.r2dbc.client.IUserServiceClient;
import com.projectArka.order_service.infrastructure.adapter.out.r2dbc.client.ProductDetailsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderManagementUseCaseTest {

    @Mock
    private IOrderRepository orderRepository;
    @Mock
    private IOrderItemRepository orderItemRepository;
    @Mock
    private IOrderAddressRepository orderAddressRepository;
    @Mock
    private IShipmentRepository shipmentRepository;
    @Mock
    private IUserServiceClient userServiceClient;
    @Mock
    private IProductServiceClient productServiceClient;
    @Mock
    private IOrderMapper orderMapper;

    @InjectMocks
    private OrderManagementUseCase orderManagementUseCase;

    // Common test data
    private UUID userId;
    private UUID orderId;
    private UUID productId1;
    private UUID productId2;
    private UUID shippingAddressId;
    private UUID billingAddressId;

    private OrderRequestDTO orderRequestDTO;
    private Order order;
    private OrderItem orderItem1;
    private OrderItem orderItem2;
    private OrderAddress shippingAddress;
    private OrderAddress billingAddress;
    private Shipment shipment;
    private ProductDetailsResponse productDetails1;
    private ProductDetailsResponse productDetails2;
    private OrderResponseDTO expectedOrderResponseDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        productId1 = UUID.randomUUID();
        productId2 = UUID.randomUUID();
        shippingAddressId = UUID.randomUUID();
        billingAddressId = UUID.randomUUID();

        // DTOs
        OrderItemRequestDTO itemRequest1 = OrderItemRequestDTO.builder()
                .productId(productId1.toString())
                .quantity(2)
                .build();
        OrderItemRequestDTO itemRequest2 = OrderItemRequestDTO.builder()
                .productId(productId2.toString())
                .quantity(1)
                .build();

        OrderAddressDTO shippingAddressDTO = OrderAddressDTO.builder()
                .street("123 Main St")
                .city("Anytown")
                .state("CA")
                .postalCode("90210")
                .country("USA")
                .build();
        OrderAddressDTO billingAddressDTO = OrderAddressDTO.builder()
                .street("456 Other Rd")
                .city("Otherville")
                .state("NY")
                .postalCode("10001")
                .country("USA")
                .build();

        orderRequestDTO = OrderRequestDTO.builder()
                .userId(userId.toString())
                .items(List.of(itemRequest1, itemRequest2))
                .shippingAddress(shippingAddressDTO)
                .billingAddress(billingAddressDTO)
                .build();

        shippingAddress = OrderAddress.builder()
                .id(shippingAddressId)
                .street("123 Main St")
                .city("Anytown")
                .state("CA")
                .postalCode("90210")
                .country("USA")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        billingAddress = OrderAddress.builder()
                .id(billingAddressId)
                .street("456 Other Rd")
                .city("Otherville")
                .state("NY")
                .postalCode("10001")
                .country("USA")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        order = Order.builder()
                .id(orderId)
                .userId(userId)
                .orderDate(Instant.now())
                .status("PENDING")
                .totalAmount(new BigDecimal("250.00"))
                .shippingAddressId(shippingAddressId)
                .billingAddressId(billingAddressId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        orderItem1 = OrderItem.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .productId(productId1)
                .quantity(2)
                .unitPrice(new BigDecimal("100.00"))
                .build();
        orderItem2 = OrderItem.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .productId(productId2)
                .quantity(1)
                .unitPrice(new BigDecimal("50.00"))
                .build();

        shipment = Shipment.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .trackingNumber("TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .status("PREPARING")
                .shippingDate(Instant.now().plusSeconds(86400 * 3))
                .build();

        productDetails1 = ProductDetailsResponse.builder()
                .productId(productId1)
                .name("Product A")
                .price(new BigDecimal("100.00"))
                .build();
        productDetails2 = ProductDetailsResponse.builder()
                .productId(productId2)
                .name("Product B")
                .price(new BigDecimal("50.00"))
                .build();

        OrderItemResponseDTO itemResponse1 = OrderItemResponseDTO.builder()
                .id(orderItem1.getId().toString())
                .productId(orderItem1.getProductId().toString())
                .productName("Product A")
                .quantity(orderItem1.getQuantity())
                .unitPrice(orderItem1.getUnitPrice())
                .build();
        OrderItemResponseDTO itemResponse2 = OrderItemResponseDTO.builder()
                .id(orderItem2.getId().toString())
                .productId(orderItem2.getProductId().toString())
                .productName("Product B")
                .quantity(orderItem2.getQuantity())
                .unitPrice(orderItem2.getUnitPrice())
                .build();

        expectedOrderResponseDTO = OrderResponseDTO.builder()
                .orderId(order.getId().toString())
                .userId(order.getUserId().toString())
                .orderDate(order.getOrderDate().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(shippingAddressDTO)
                .billingAddress(billingAddressDTO)
                .items(List.of(itemResponse1, itemResponse2))
                .shipment(ShipmentDTO.builder()
                        .idShipment(shipment.getId().toString())
                        .orderId(shipment.getOrderId().toString())
                        .trackingNumber(shipment.getTrackingNumber())
                        .status(shipment.getStatus())
                        .shippingDate(shipment.getShippingDate().atZone(ZoneId.systemDefault()).toLocalDateTime())
                        .createdAt(shipment.getCreatedAt() != null ? shipment.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime() : null)
                        .updatedAt(shipment.getUpdatedAt() != null ? shipment.getUpdatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime() : null)
                        .build())
                .createdAt(order.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .updatedAt(order.getUpdatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .build();

        lenient().when(orderMapper.toOrderAddress(any(OrderAddressDTO.class))).thenReturn(shippingAddress);
        lenient().when(orderMapper.toOrderAddressDTO(shippingAddress)).thenReturn(shippingAddressDTO);
        lenient().when(orderMapper.toOrderAddressDTO(billingAddress)).thenReturn(billingAddressDTO);

        lenient().when(orderMapper.toOrderItem(any(OrderItemRequestDTO.class)))
                .thenAnswer(invocation -> {
                    OrderItemRequestDTO dto = invocation.getArgument(0);
                    return OrderItem.builder()
                            .productId(UUID.fromString(dto.getProductId()))
                            .quantity(dto.getQuantity())
                            .build();
                });

        lenient().when(orderMapper.toOrderResponseDTO(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order orderArg = invocation.getArgument(0);
                    return OrderResponseDTO.builder()
                            .orderId(orderArg.getId().toString())
                            .userId(orderArg.getUserId().toString())
                            .orderDate(orderArg.getOrderDate().atZone(ZoneId.systemDefault()).toLocalDateTime())
                            .status(orderArg.getStatus())
                            .totalAmount(orderArg.getTotalAmount())
                            .createdAt(orderArg.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime())
                            .updatedAt(orderArg.getUpdatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime())
                            .build();
                });

        lenient().when(orderMapper.toOrderItemResponseDTO(orderItem1)).thenReturn(itemResponse1);
        lenient().when(orderMapper.toOrderItemResponseDTO(orderItem2)).thenReturn(itemResponse2);
        lenient().when(orderMapper.toOrderItemResponseDTO(any(OrderItem.class)))
                .thenAnswer(invocation -> {
                    OrderItem item = invocation.getArgument(0);
                    return OrderItemResponseDTO.builder()
                            .id(item.getId() != null ? item.getId().toString() : null)
                            .productId(item.getProductId() != null ? item.getProductId().toString() : null)
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .build();
                });

        lenient().when(orderMapper.toShipmentDTO(any(Shipment.class)))
                .thenAnswer(invocation -> {
                    Shipment shipArg = invocation.getArgument(0);
                    return ShipmentDTO.builder()
                            .idShipment(shipArg.getId() != null ? shipArg.getId().toString() : null)
                            .orderId(shipArg.getOrderId() != null ? shipArg.getOrderId().toString() : null)
                            .trackingNumber(shipArg.getTrackingNumber())
                            .status(shipArg.getStatus())
                            .shippingDate(shipArg.getShippingDate() != null ? shipArg.getShippingDate().atZone(ZoneId.systemDefault()).toLocalDateTime() : null)
                            .createdAt(shipArg.getCreatedAt() != null ? shipArg.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime() : null)
                            .updatedAt(shipArg.getUpdatedAt() != null ? shipArg.getUpdatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime() : null)
                            .build();
                });
    }

    @Test
    @DisplayName("Should throw InvalidOrderDataException for invalid userId format")
    void createOrder_invalidUserIdFormat() {
        orderRequestDTO.setUserId("invalid-uuid");

        StepVerifier.create(orderManagementUseCase.createOrder(orderRequestDTO))
                .expectErrorMatches(e -> e instanceof InvalidOrderDataException &&
                        e.getMessage().contains("Invalid user ID format"))
                .verify();

        verify(userServiceClient, never()).validateUserExists(any(UUID.class));
    }

    @Test
    @DisplayName("Should throw UserNotFoundException if user does not exist")
    void createOrder_userNotFound() {
        when(userServiceClient.validateUserExists(userId)).thenReturn(Mono.just(false));

        StepVerifier.create(orderManagementUseCase.createOrder(orderRequestDTO))
                .expectErrorMatches(e -> e instanceof UserNotFoundException &&
                        e.getMessage().contains("User with ID " + userId + " not found."))
                .verify();

        verify(userServiceClient, times(1)).validateUserExists(userId);
        verify(productServiceClient, never()).getProductDetails(any(UUID.class));
    }

    @Test
    @DisplayName("Should throw InvalidOrderDataException for invalid productId format")
    void createOrder_invalidProductIdFormat() {
        OrderItemRequestDTO invalidItem = OrderItemRequestDTO.builder()
                .productId("bad-product-uuid")
                .quantity(1)
                .build();
        orderRequestDTO.setItems(List.of(invalidItem));

        when(userServiceClient.validateUserExists(userId)).thenReturn(Mono.just(true));

        StepVerifier.create(orderManagementUseCase.createOrder(orderRequestDTO))
                .expectErrorMatches(e -> e instanceof InvalidOrderDataException &&
                        e.getMessage().contains("Invalid product ID format"))
                .verify();
    }

    @Test
    @DisplayName("Should throw InvalidOrderDataException if product not found")
    void createOrder_productNotFound() {
        when(userServiceClient.validateUserExists(userId)).thenReturn(Mono.just(true));
        when(productServiceClient.getProductDetails(productId1)).thenReturn(Mono.empty());

        orderRequestDTO.setItems(List.of(OrderItemRequestDTO.builder().productId(productId1.toString()).quantity(1).build()));

        StepVerifier.create(orderManagementUseCase.createOrder(orderRequestDTO))
                .expectErrorMatches(e -> e instanceof InvalidOrderDataException &&
                        e.getMessage().contains("Product with ID " + productId1 + " not found."))
                .verify();

        verify(productServiceClient, times(1)).getProductDetails(productId1);
        verify(productServiceClient, never()).checkProductStock(any(UUID.class), anyInt());
    }

    @Test
    @DisplayName("Should throw InsufficientStockException if stock is not available")
    void createOrder_insufficientStock() {
        when(userServiceClient.validateUserExists(userId)).thenReturn(Mono.just(true));
        when(productServiceClient.getProductDetails(productId1)).thenReturn(Mono.just(productDetails1));
        when(productServiceClient.checkProductStock(eq(productId1), anyInt())).thenReturn(Mono.just(Map.of("available", false)));

        orderRequestDTO.setItems(List.of(OrderItemRequestDTO.builder().productId(productId1.toString()).quantity(5).build()));

        StepVerifier.create(orderManagementUseCase.createOrder(orderRequestDTO))
                .expectErrorMatches(e -> e instanceof InsufficientStockException &&
                        e.getMessage().contains("Insufficient stock for product ID: " + productId1))
                .verify();

        verify(productServiceClient, times(1)).getProductDetails(productId1);
        verify(productServiceClient, times(1)).checkProductStock(productId1, 5);
        verify(orderRepository, never()).save(any(Order.class)); // Should not save order
    }

    @Test
    @DisplayName("Should throw InvalidOrderDataException if order items are empty")
    void createOrder_emptyItems() {
        orderRequestDTO.setItems(Collections.emptyList());
        when(userServiceClient.validateUserExists(userId)).thenReturn(Mono.just(true));

        StepVerifier.create(orderManagementUseCase.createOrder(orderRequestDTO))
                .expectErrorMatches(e -> e instanceof InvalidOrderDataException &&
                        e.getMessage().contains("Order must contain at least one valid item."))
                .verify();

        verify(userServiceClient, times(1)).validateUserExists(userId);
        verify(productServiceClient, never()).getProductDetails(any(UUID.class));
    }

    @Test
    @DisplayName("Should retrieve an order by ID successfully")
    void getOrderById_success() {
        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));
        when(orderAddressRepository.findById(shippingAddressId)).thenReturn(Mono.just(shippingAddress));
        when(orderAddressRepository.findById(billingAddressId)).thenReturn(Mono.just(billingAddress));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Flux.just(orderItem1, orderItem2));
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Mono.just(shipment));
        when(productServiceClient.getProductDetails(productId1)).thenReturn(Mono.just(productDetails1));
        when(productServiceClient.getProductDetails(productId2)).thenReturn(Mono.just(productDetails2));

        StepVerifier.create(orderManagementUseCase.getOrderById(orderId.toString()))
                .expectNext(expectedOrderResponseDTO)
                .verifyComplete();

        verify(orderRepository, times(1)).findById(orderId);
        verify(orderAddressRepository, times(1)).findById(shippingAddressId);
        verify(orderAddressRepository, times(1)).findById(billingAddressId);
        verify(orderItemRepository, times(1)).findByOrderId(orderId);
        verify(shipmentRepository, times(1)).findByOrderId(orderId);
        verify(productServiceClient, times(1)).getProductDetails(productId1);
        verify(productServiceClient, times(1)).getProductDetails(productId2);
    }

    @Test
    @DisplayName("Should throw OrderNotFoundException if order by ID not found")
    void getOrderById_notFound() {
        when(orderRepository.findById(orderId)).thenReturn(Mono.empty());

        StepVerifier.create(orderManagementUseCase.getOrderById(orderId.toString()))
                .expectErrorMatches(e -> e instanceof OrderNotFoundException &&
                        e.getMessage().contains("Order with ID " + orderId + " not found."))
                .verify();
    }

    @Test
    @DisplayName("Should throw InvalidOrderDataException for invalid orderId format")
    void getOrderById_invalidIdFormat() {
        StepVerifier.create(orderManagementUseCase.getOrderById("invalid-uuid"))
                .expectErrorMatches(e -> e instanceof InvalidOrderDataException &&
                        e.getMessage().contains("Invalid order ID format"))
                .verify();
    }

    @Test
    @DisplayName("Should handle product details unavailability when building response for getOrderById")
    void getOrderById_productDetailsUnavailable() {
        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));
        when(orderAddressRepository.findById(shippingAddressId)).thenReturn(Mono.just(shippingAddress));
        when(orderAddressRepository.findById(billingAddressId)).thenReturn(Mono.just(billingAddress));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Flux.just(orderItem1));
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Mono.just(shipment));
        when(productServiceClient.getProductDetails(productId1)).thenReturn(Mono.error(new RuntimeException("Product service down")));

        lenient().when(orderMapper.toOrderItemResponseDTO(orderItem1))
                .thenReturn(OrderItemResponseDTO.builder()
                        .id(orderItem1.getId().toString())
                        .productId(orderItem1.getProductId().toString())
                        .quantity(orderItem1.getQuantity())
                        .unitPrice(orderItem1.getUnitPrice())
                        .productName("Product Name Unavailable")
                        .build());


        StepVerifier.create(orderManagementUseCase.getOrderById(orderId.toString()))
                .expectNextMatches(response -> {
                    assertEquals(orderId.toString(), response.getOrderId());
                    assertEquals("Product Name Unavailable", response.getItems().get(0).getProductName());
                    return true;
                })
                .verifyComplete();

        verify(productServiceClient, times(1)).getProductDetails(productId1);
    }


    // --- getOrdersByUserId tests ---

    @Test
    @DisplayName("Should retrieve orders by user ID successfully")
    void getOrdersByUserId_success() {
        Order anotherOrder = Order.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .orderDate(Instant.now())
                .status("COMPLETED")
                .totalAmount(new BigDecimal("75.00"))
                .shippingAddressId(UUID.randomUUID())
                .billingAddressId(UUID.randomUUID())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        OrderAddress anotherShippingAddress = OrderAddress.builder().id(anotherOrder.getShippingAddressId())
                .street("Another St").city("Another City").state("CA").postalCode("90210").country("USA")
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
        OrderAddress anotherBillingAddress = OrderAddress.builder().id(anotherOrder.getBillingAddressId())
                .street("Billing St").city("Billing City").state("NY").postalCode("10001").country("USA")
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();

        OrderItem anotherOrderItem = OrderItem.builder()
                .id(UUID.randomUUID())
                .orderId(anotherOrder.getId())
                .productId(UUID.randomUUID())
                .quantity(1)
                .unitPrice(new BigDecimal("75.00"))
                .build();
        Shipment anotherShipment = Shipment.builder()
                .id(UUID.randomUUID())
                .orderId(anotherOrder.getId())
                .trackingNumber("TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .status("SHIPPED")
                .shippingDate(Instant.now().plusSeconds(86400 * 5))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        ProductDetailsResponse anotherProductDetails = ProductDetailsResponse.builder().productId(anotherOrderItem.getProductId()).name("Another Product").price(new BigDecimal("75.00")).build();

        OrderItemResponseDTO anotherItemResponse = OrderItemResponseDTO.builder()
                .id(anotherOrderItem.getId().toString())
                .productId(anotherOrderItem.getProductId().toString())
                .productName("Another Product")
                .quantity(anotherOrderItem.getQuantity())
                .unitPrice(anotherOrderItem.getUnitPrice())
                .build();

        OrderResponseDTO anotherExpectedOrderResponseDTO = OrderResponseDTO.builder()
                .orderId(anotherOrder.getId().toString())
                .userId(anotherOrder.getUserId().toString())
                .orderDate(anotherOrder.getOrderDate().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .status("COMPLETED")
                .totalAmount(anotherOrder.getTotalAmount())
                .shippingAddress(orderMapper.toOrderAddressDTO(anotherShippingAddress))
                .billingAddress(orderMapper.toOrderAddressDTO(anotherBillingAddress))
                .items(List.of(anotherItemResponse))
                .shipment(orderMapper.toShipmentDTO(anotherShipment))
                .createdAt(anotherOrder.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .updatedAt(anotherOrder.getUpdatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .build();

        when(orderRepository.findByUserId(userId)).thenReturn(Flux.just(order, anotherOrder));
        when(orderAddressRepository.findById(order.getShippingAddressId())).thenReturn(Mono.just(shippingAddress));
        when(orderAddressRepository.findById(order.getBillingAddressId())).thenReturn(Mono.just(billingAddress));
        when(orderItemRepository.findByOrderId(order.getId())).thenReturn(Flux.just(orderItem1, orderItem2));
        when(shipmentRepository.findByOrderId(order.getId())).thenReturn(Mono.just(shipment));
        when(productServiceClient.getProductDetails(orderItem1.getProductId())).thenReturn(Mono.just(productDetails1));
        when(productServiceClient.getProductDetails(orderItem2.getProductId())).thenReturn(Mono.just(productDetails2));


        when(orderAddressRepository.findById(anotherOrder.getShippingAddressId())).thenReturn(Mono.just(anotherShippingAddress));
        when(orderAddressRepository.findById(anotherOrder.getBillingAddressId())).thenReturn(Mono.just(anotherBillingAddress));
        when(orderItemRepository.findByOrderId(anotherOrder.getId())).thenReturn(Flux.just(anotherOrderItem));
        when(shipmentRepository.findByOrderId(anotherOrder.getId())).thenReturn(Mono.just(anotherShipment));
        when(productServiceClient.getProductDetails(anotherOrderItem.getProductId())).thenReturn(Mono.just(anotherProductDetails));

        lenient().when(orderMapper.toOrderResponseDTO(order)).thenReturn(expectedOrderResponseDTO);
        lenient().when(orderMapper.toOrderResponseDTO(anotherOrder)).thenReturn(anotherExpectedOrderResponseDTO);
        lenient().when(orderMapper.toOrderAddressDTO(anotherShippingAddress)).thenReturn(OrderAddressDTO.builder()
                .street("Another St").city("Another City").state("CA").postalCode("90210").country("USA").build());
        lenient().when(orderMapper.toOrderAddressDTO(anotherBillingAddress)).thenReturn(OrderAddressDTO.builder()
                .street("Billing St").city("Billing City").state("NY").postalCode("10001").country("USA").build());

        lenient().when(orderMapper.toShipmentDTO(anotherShipment)).thenReturn(ShipmentDTO.builder()
                .idShipment(anotherShipment.getId().toString())
                .orderId(anotherShipment.getOrderId().toString())
                .trackingNumber(anotherShipment.getTrackingNumber())
                .status(anotherShipment.getStatus())
                .shippingDate(anotherShipment.getShippingDate().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .createdAt(anotherShipment.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .updatedAt(anotherShipment.getUpdatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .build());

        lenient().when(orderMapper.toOrderItemResponseDTO(anotherOrderItem)).thenReturn(anotherExpectedOrderResponseDTO.getItems().get(0));


        StepVerifier.create(orderManagementUseCase.getOrdersByUserId(userId.toString()))
                .expectNext(expectedOrderResponseDTO, anotherExpectedOrderResponseDTO)
                .verifyComplete();

        verify(orderRepository, times(1)).findByUserId(userId);
        verify(orderAddressRepository, times(1)).findById(order.getShippingAddressId());
        verify(orderAddressRepository, times(1)).findById(order.getBillingAddressId());
        verify(orderItemRepository, times(1)).findByOrderId(order.getId());
        verify(shipmentRepository, times(1)).findByOrderId(order.getId());
        verify(productServiceClient, times(1)).getProductDetails(orderItem1.getProductId());
        verify(productServiceClient, times(1)).getProductDetails(orderItem2.getProductId());

        verify(orderAddressRepository, times(1)).findById(anotherOrder.getShippingAddressId());
        verify(orderAddressRepository, times(1)).findById(anotherOrder.getBillingAddressId());
        verify(orderItemRepository, times(1)).findByOrderId(anotherOrder.getId());
        verify(shipmentRepository, times(1)).findByOrderId(anotherOrder.getId());
        verify(productServiceClient, times(1)).getProductDetails(anotherOrderItem.getProductId());
    }

    @Test
    @DisplayName("Should throw InvalidOrderDataException for invalid userId format when fetching by user ID")
    void getOrdersByUserId_invalidIdFormat() {
        StepVerifier.create(orderManagementUseCase.getOrdersByUserId("invalid-uuid"))
                .expectErrorMatches(e -> e instanceof InvalidOrderDataException &&
                        e.getMessage().contains("Invalid user ID format"))
                .verify();
    }

    @Test
    @DisplayName("Should return empty Flux if no orders found for user ID")
    void getOrdersByUserId_noOrdersFound() {
        when(orderRepository.findByUserId(userId)).thenReturn(Flux.empty());

        StepVerifier.create(orderManagementUseCase.getOrdersByUserId(userId.toString()))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should throw OrderNotFoundException if order not found for status update")
    void updateOrderStatus_notFound() {
        when(orderRepository.findById(orderId)).thenReturn(Mono.empty());

        StepVerifier.create(orderManagementUseCase.updateOrderStatus(orderId.toString(), "SHIPPED"))
                .expectErrorMatches(e -> e instanceof OrderNotFoundException)
                .verify();
    }

    @Test
    @DisplayName("Should throw InvalidOrderDataException for invalid orderId format for status update")
    void updateOrderStatus_invalidIdFormat() {
        StepVerifier.create(orderManagementUseCase.updateOrderStatus("invalid-uuid", "SHIPPED"))
                .expectErrorMatches(e -> e instanceof InvalidOrderDataException)
                .verify();
    }


    @Test
    @DisplayName("Should cancel order and increase stock successfully if not CANCELLED or DELIVERED")
    void cancelOrder_success() {
        order.setStatus("PENDING");
        Order cancelledOrder = order.toBuilder().status("CANCELLED").updatedAt(Instant.now()).build();

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(cancelledOrder));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Flux.just(orderItem1, orderItem2));
        when(productServiceClient.increaseProductStock(productId1, 2)).thenReturn(Mono.empty());
        when(productServiceClient.increaseProductStock(productId2, 1)).thenReturn(Mono.empty());

        StepVerifier.create(orderManagementUseCase.cancelOrder(orderId.toString()))
                .verifyComplete();

        verify(orderRepository, times(1)).findById(orderId);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderItemRepository, times(1)).findByOrderId(orderId);
        verify(productServiceClient, times(1)).increaseProductStock(productId1, 2);
        verify(productServiceClient, times(1)).increaseProductStock(productId2, 1);
    }

    @Test
    @DisplayName("Should not cancel order if status is already CANCELLED")
    void cancelOrder_alreadyCancelled() {
        order.setStatus("CANCELLED");
        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));

        StepVerifier.create(orderManagementUseCase.cancelOrder(orderId.toString()))
                .verifyComplete();

        verify(orderRepository, never()).save(any(Order.class));
        verify(orderItemRepository, never()).findByOrderId(any(UUID.class));
        verify(productServiceClient, never()).increaseProductStock(any(UUID.class), anyInt());
    }

    @Test
    @DisplayName("Should not cancel order if status is DELIVERED")
    void cancelOrder_delivered() {
        order.setStatus("DELIVERED");
        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));

        StepVerifier.create(orderManagementUseCase.cancelOrder(orderId.toString()))
                .verifyComplete();

        verify(orderRepository, never()).save(any(Order.class));
        verify(orderItemRepository, never()).findByOrderId(any(UUID.class));
        verify(productServiceClient, never()).increaseProductStock(any(UUID.class), anyInt());
    }

    @Test
    @DisplayName("Should throw OrderNotFoundException if order not found for cancellation")
    void cancelOrder_notFound() {
        when(orderRepository.findById(orderId)).thenReturn(Mono.empty());

        StepVerifier.create(orderManagementUseCase.cancelOrder(orderId.toString()))
                .expectErrorMatches(e -> e instanceof OrderNotFoundException)
                .verify();
    }

    @Test
    @DisplayName("Should throw InvalidOrderDataException for invalid orderId format for cancellation")
    void cancelOrder_invalidIdFormat() {
        StepVerifier.create(orderManagementUseCase.cancelOrder("invalid-uuid"))
                .expectErrorMatches(e -> e instanceof InvalidOrderDataException)
                .verify();
    }

    @Test
    @DisplayName("Should retrieve all orders successfully")
    void getAllOrders_success() {
        Order anotherOrder = Order.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .orderDate(Instant.now())
                .status("COMPLETED")
                .totalAmount(new BigDecimal("75.00"))
                .shippingAddressId(UUID.randomUUID())
                .billingAddressId(UUID.randomUUID())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        OrderAddress anotherShippingAddress = OrderAddress.builder().id(anotherOrder.getShippingAddressId())
                .street("Another St").city("Another City").state("CA").postalCode("90210").country("USA")
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
        OrderAddress anotherBillingAddress = OrderAddress.builder().id(anotherOrder.getBillingAddressId())
                .street("Billing St").city("Billing City").state("NY").postalCode("10001").country("USA")
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
        OrderItem anotherOrderItem = OrderItem.builder()
                .id(UUID.randomUUID())
                .orderId(anotherOrder.getId()).productId(UUID.randomUUID()).quantity(1).unitPrice(new BigDecimal("75.00")).build();
        Shipment anotherShipment = Shipment.builder()
                .id(UUID.randomUUID())
                .orderId(anotherOrder.getId())
                .trackingNumber("TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .status("SHIPPED")
                .shippingDate(Instant.now().plusSeconds(86400 * 5))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        ProductDetailsResponse anotherProductDetails = ProductDetailsResponse.builder().productId(anotherOrderItem.getProductId()).name("Another Product").price(new BigDecimal("75.00")).build();

        OrderItemResponseDTO anotherItemResponse = OrderItemResponseDTO.builder()
                .id(anotherOrderItem.getId().toString())
                .productId(anotherOrderItem.getProductId().toString())
                .productName("Another Product")
                .quantity(anotherOrderItem.getQuantity())
                .unitPrice(anotherOrderItem.getUnitPrice())
                .build();

        OrderResponseDTO anotherExpectedOrderResponseDTO = OrderResponseDTO.builder()
                .orderId(anotherOrder.getId().toString())
                .userId(anotherOrder.getUserId().toString())
                .orderDate(anotherOrder.getOrderDate().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .status("COMPLETED")
                .totalAmount(anotherOrder.getTotalAmount())
                .shippingAddress(orderMapper.toOrderAddressDTO(anotherShippingAddress))
                .billingAddress(orderMapper.toOrderAddressDTO(anotherBillingAddress))
                .items(List.of(anotherItemResponse))
                .shipment(orderMapper.toShipmentDTO(anotherShipment))
                .createdAt(anotherOrder.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .updatedAt(anotherOrder.getUpdatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .build();

        when(orderRepository.findAll()).thenReturn(Flux.just(order, anotherOrder));

        when(orderAddressRepository.findById(order.getShippingAddressId())).thenReturn(Mono.just(shippingAddress));
        when(orderAddressRepository.findById(order.getBillingAddressId())).thenReturn(Mono.just(billingAddress));
        when(orderItemRepository.findByOrderId(order.getId())).thenReturn(Flux.just(orderItem1, orderItem2));
        when(shipmentRepository.findByOrderId(order.getId())).thenReturn(Mono.just(shipment));
        when(productServiceClient.getProductDetails(orderItem1.getProductId())).thenReturn(Mono.just(productDetails1));
        when(productServiceClient.getProductDetails(orderItem2.getProductId())).thenReturn(Mono.just(productDetails2));

        when(orderAddressRepository.findById(anotherOrder.getShippingAddressId())).thenReturn(Mono.just(anotherShippingAddress));
        when(orderAddressRepository.findById(anotherOrder.getBillingAddressId())).thenReturn(Mono.just(anotherBillingAddress));
        when(orderItemRepository.findByOrderId(anotherOrder.getId())).thenReturn(Flux.just(anotherOrderItem));
        when(shipmentRepository.findByOrderId(anotherOrder.getId())).thenReturn(Mono.just(anotherShipment));
        when(productServiceClient.getProductDetails(anotherOrderItem.getProductId())).thenReturn(Mono.just(anotherProductDetails));

        lenient().when(orderMapper.toOrderResponseDTO(order)).thenReturn(expectedOrderResponseDTO);
        lenient().when(orderMapper.toOrderResponseDTO(anotherOrder)).thenReturn(anotherExpectedOrderResponseDTO);
        lenient().when(orderMapper.toOrderAddressDTO(anotherShippingAddress)).thenReturn(OrderAddressDTO.builder()
                .street("Another St").city("Another City").state("CA").postalCode("90210").country("USA").build());
        lenient().when(orderMapper.toOrderAddressDTO(anotherBillingAddress)).thenReturn(OrderAddressDTO.builder()
                .street("Billing St").city("Billing City").state("NY").postalCode("10001").country("USA").build());
        lenient().when(orderMapper.toShipmentDTO(anotherShipment)).thenReturn(ShipmentDTO.builder()
                .idShipment(anotherShipment.getId().toString())
                .orderId(anotherShipment.getOrderId().toString())
                .trackingNumber(anotherShipment.getTrackingNumber())
                .status(anotherShipment.getStatus())
                .shippingDate(anotherShipment.getShippingDate().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .createdAt(anotherShipment.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .updatedAt(anotherShipment.getUpdatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .build());
        lenient().when(orderMapper.toOrderItemResponseDTO(anotherOrderItem)).thenReturn(anotherExpectedOrderResponseDTO.getItems().get(0));

        StepVerifier.create(orderManagementUseCase.getAllOrders())
                .expectNext(expectedOrderResponseDTO, anotherExpectedOrderResponseDTO)
                .verifyComplete();

        verify(orderRepository, times(1)).findAll();
        verify(orderAddressRepository, times(1)).findById(order.getShippingAddressId());
        verify(orderAddressRepository, times(1)).findById(order.getBillingAddressId());
        verify(orderItemRepository, times(1)).findByOrderId(order.getId());
        verify(shipmentRepository, times(1)).findByOrderId(order.getId());
        verify(productServiceClient, times(1)).getProductDetails(orderItem1.getProductId());
        verify(productServiceClient, times(1)).getProductDetails(orderItem2.getProductId());

        verify(orderAddressRepository, times(1)).findById(anotherOrder.getShippingAddressId());
        verify(orderAddressRepository, times(1)).findById(anotherOrder.getBillingAddressId());
        verify(orderItemRepository, times(1)).findByOrderId(anotherOrder.getId());
        verify(shipmentRepository, times(1)).findByOrderId(anotherOrder.getId());
        verify(productServiceClient, times(1)).getProductDetails(anotherOrderItem.getProductId());
    }

    @Test
    @DisplayName("Should return empty Flux if no orders are found")
    void getAllOrders_noOrdersFound() {
        when(orderRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(orderManagementUseCase.getAllOrders())
                .expectNextCount(0)
                .verifyComplete();
    }
}