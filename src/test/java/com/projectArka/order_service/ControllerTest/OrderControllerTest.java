package com.projectArka.order_service.ControllerTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectArka.order_service.OrderServiceApplication;
import com.projectArka.order_service.application.dto.OrderAddressDTO;
import com.projectArka.order_service.application.dto.OrderItemRequestDTO;
import com.projectArka.order_service.application.dto.OrderItemResponseDTO;
import com.projectArka.order_service.application.dto.OrderRequestDTO;
import com.projectArka.order_service.application.dto.OrderResponseDTO;
import com.projectArka.order_service.application.dto.ShipmentDTO;
import com.projectArka.order_service.application.usecase.OrderManagementUseCase;
import com.projectArka.order_service.domain.exception.InvalidOrderDataException;
import com.projectArka.order_service.domain.exception.OrderNotFoundException;
import com.projectArka.order_service.infrastructure.adapter.in.webflux.OrderController;
import com.projectArka.order_service.infrastructure.config.R2dbcConfig;
import com.projectArka.order_service.infrastructure.config.GlobalExceptionHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebFluxTest(
        controllers = OrderController.class,
        includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = GlobalExceptionHandler.class),
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = OrderServiceApplication.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = R2dbcConfig.class),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.projectArka\\.order_service\\.infrastructure\\.adapter\\.out\\..*")
        }
)
class OrderControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private OrderManagementUseCase orderManagementUseCase;

    @Autowired
    private ObjectMapper objectMapper;

    private OrderRequestDTO orderRequestDTO;
    private OrderResponseDTO orderResponseDTO;
    private String orderId;
    private String userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
        orderId = UUID.randomUUID().toString();

        OrderItemRequestDTO itemRequest1 = OrderItemRequestDTO.builder()
                .productId(UUID.randomUUID().toString())
                .quantity(2)
                .build();
        OrderItemRequestDTO itemRequest2 = OrderItemRequestDTO.builder()
                .productId(UUID.randomUUID().toString())
                .quantity(1)
                .build();

        OrderAddressDTO shippingAddressDTO = OrderAddressDTO.builder()
                .street("123 Main St")
                .number("42")
                .city("Anytown")
                .state("CA")
                .postalCode("90210")
                .country("USA")
                .build();
        OrderAddressDTO billingAddressDTO = OrderAddressDTO.builder()
                .street("456 Other Rd")
                .number("10")
                .city("Otherville")
                .state("NY")
                .postalCode("10001")
                .country("USA")
                .build();

        orderRequestDTO = OrderRequestDTO.builder()
                .userId(userId)
                .items(List.of(itemRequest1, itemRequest2))
                .shippingAddress(shippingAddressDTO)
                .billingAddress(billingAddressDTO)
                .build();

        OrderItemResponseDTO itemResponse1 = OrderItemResponseDTO.builder()
                .id(UUID.randomUUID().toString())
                .productId(itemRequest1.getProductId())
                .productName("Product A")
                .quantity(itemRequest1.getQuantity())
                .unitPrice(new BigDecimal("100.00"))
                .build();
        OrderItemResponseDTO itemResponse2 = OrderItemResponseDTO.builder()
                .id(UUID.randomUUID().toString())
                .productId(itemRequest2.getProductId())
                .productName("Product B")
                .quantity(itemRequest2.getQuantity())
                .unitPrice(new BigDecimal("50.00"))
                .build();

        ShipmentDTO shipmentDTO = ShipmentDTO.builder()
                .idShipment(UUID.randomUUID().toString())
                .orderId(orderId)
                .trackingNumber("TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .status("PREPARING")
                .shippingDate(LocalDateTime.now().plusDays(3))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        orderResponseDTO = OrderResponseDTO.builder()
                .orderId(orderId)
                .userId(userId)
                .orderDate(LocalDateTime.now())
                .status("PENDING")
                .totalAmount(new BigDecimal("250.00"))
                .shippingAddress(shippingAddressDTO)
                .billingAddress(billingAddressDTO)
                .items(List.of(itemResponse1, itemResponse2))
                .shipment(shipmentDTO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should create an order successfully and return 201 CREATED")
    void createOrder_success() throws Exception {
        when(orderManagementUseCase.createOrder(any(OrderRequestDTO.class)))
                .thenReturn(Mono.just(orderResponseDTO));

        webTestClient.post().uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(orderRequestDTO))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(OrderResponseDTO.class)
                .isEqualTo(orderResponseDTO);
    }

    @Test
    @DisplayName("Should return 500 INTERNAL SERVER ERROR for unexpected errors during order creation")
    void createOrder_unexpectedError() throws Exception {
        when(orderManagementUseCase.createOrder(any(OrderRequestDTO.class)))
                .thenReturn(Mono.error(new RuntimeException("Database connection failed")));

        webTestClient.post().uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(orderRequestDTO))
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    @DisplayName("Should retrieve an order by ID successfully and return 200 OK")
    void getOrderById_success() {
        when(orderManagementUseCase.getOrderById(orderId))
                .thenReturn(Mono.just(orderResponseDTO));

        webTestClient.get().uri("/api/orders/{orderId}", orderId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(OrderResponseDTO.class)
                .isEqualTo(orderResponseDTO);
    }

    @Test
    @DisplayName("Should return 404 NOT FOUND for non-existent order ID")
    void getOrderById_notFound() {
        when(orderManagementUseCase.getOrderById(anyString()))
                .thenReturn(Mono.error(new OrderNotFoundException("Order not found")));

        webTestClient.get().uri("/api/orders/{orderId}", orderId)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("Should return 500 INTERNAL SERVER ERROR for invalid order ID format (as per current controller handling)")
    void getOrderById_invalidIdFormat() {
        when(orderManagementUseCase.getOrderById(anyString()))
                .thenReturn(Mono.error(new InvalidOrderDataException("Invalid order ID format")));

        webTestClient.get().uri("/api/orders/{orderId}", "invalid-uuid")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    @DisplayName("Should return 500 INTERNAL SERVER ERROR for unexpected errors fetching order by ID")
    void getOrderById_unexpectedError() {
        when(orderManagementUseCase.getOrderById(anyString()))
                .thenReturn(Mono.error(new RuntimeException("Database error")));

        webTestClient.get().uri("/api/orders/{orderId}", orderId)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    @DisplayName("Should retrieve orders by user ID successfully and return 200 OK")
    void getOrdersByUserId_success() {
        when(orderManagementUseCase.getOrdersByUserId(userId))
                .thenReturn(Flux.just(orderResponseDTO, orderResponseDTO));

        webTestClient.get().uri("/api/orders/user/{userId}", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(OrderResponseDTO.class)
                .hasSize(2)
                .contains(orderResponseDTO);
    }

    @Test
    @DisplayName("Should return 200 OK with empty list if no orders found for user ID")
    void getOrdersByUserId_noOrdersFound() {
        when(orderManagementUseCase.getOrdersByUserId(userId))
                .thenReturn(Flux.empty());

        webTestClient.get().uri("/api/orders/user/{userId}", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(OrderResponseDTO.class)
                .hasSize(0);
    }

    @Test
    @DisplayName("Should return 500 INTERNAL SERVER ERROR for unexpected errors fetching orders by user ID")
    void getOrdersByUserId_unexpectedError() {
        when(orderManagementUseCase.getOrdersByUserId(anyString()))
                .thenReturn(Flux.error(new RuntimeException("Service unavailable")));

        webTestClient.get().uri("/api/orders/user/{userId}", userId)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    @DisplayName("Should update order status successfully and return 200 OK")
    void updateOrderStatus_success() {
        String newStatus = "SHIPPED";
        OrderResponseDTO updatedOrderResponseDTO = orderResponseDTO.toBuilder().status(newStatus).build();

        when(orderManagementUseCase.updateOrderStatus(orderId, newStatus))
                .thenReturn(Mono.just(updatedOrderResponseDTO));

        webTestClient.put().uri(uriBuilder -> uriBuilder.path("/api/orders/{orderId}/status")
                        .queryParam("newStatus", newStatus)
                        .build(orderId))
                .exchange()
                .expectStatus().isOk()
                .expectBody(OrderResponseDTO.class)
                .isEqualTo(updatedOrderResponseDTO);
    }

    @Test
    @DisplayName("Should return 404 NOT FOUND when updating status for non-existent order")
    void updateOrderStatus_orderNotFound() {
        String newStatus = "SHIPPED";
        when(orderManagementUseCase.updateOrderStatus(anyString(), anyString()))
                .thenReturn(Mono.error(new OrderNotFoundException("Order not found for status update")));

        webTestClient.put().uri(uriBuilder -> uriBuilder.path("/api/orders/{orderId}/status")
                        .queryParam("newStatus", newStatus)
                        .build(orderId))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("Should return 500 INTERNAL SERVER ERROR for unexpected errors updating order status")
    void updateOrderStatus_unexpectedError() {
        String newStatus = "SHIPPED";
        when(orderManagementUseCase.updateOrderStatus(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Network error")));

        webTestClient.put().uri(uriBuilder -> uriBuilder.path("/api/orders/{orderId}/status")
                        .queryParam("newStatus", newStatus)
                        .build(orderId))
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    @DisplayName("Should cancel an order successfully and return 204 NO CONTENT")
    void cancelOrder_success() {
        when(orderManagementUseCase.cancelOrder(orderId))
                .thenReturn(Mono.empty());

        webTestClient.delete().uri("/api/orders/{orderId}", orderId)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("Should return 500 INTERNAL SERVER ERROR for unexpected errors during order cancellation")
    void cancelOrder_unexpectedError() {
        when(orderManagementUseCase.cancelOrder(anyString()))
                .thenReturn(Mono.error(new RuntimeException("Service failure")));

        webTestClient.delete().uri("/api/orders/{orderId}", orderId)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    @DisplayName("Should retrieve all orders successfully and return 200 OK")
    void getAllOrders_success() {
        when(orderManagementUseCase.getAllOrders())
                .thenReturn(Flux.just(orderResponseDTO, orderResponseDTO.toBuilder().orderId(UUID.randomUUID().toString()).build()));

        webTestClient.get().uri("/api/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(OrderResponseDTO.class)
                .hasSize(2);
    }

    @Test
    @DisplayName("Should return 200 OK with empty list if no orders exist")
    void getAllOrders_noOrdersExist() {
        when(orderManagementUseCase.getAllOrders())
                .thenReturn(Flux.empty());

        webTestClient.get().uri("/api/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(OrderResponseDTO.class)
                .hasSize(0);
    }

    @Test
    @DisplayName("Should return 500 INTERNAL SERVER ERROR for unexpected errors fetching all orders")
    void getAllOrders_unexpectedError() {
        when(orderManagementUseCase.getAllOrders())
                .thenReturn(Flux.error(new RuntimeException("Database connection lost")));

        webTestClient.get().uri("/api/orders")
                .exchange()
                .expectStatus().is5xxServerError();
    }
}