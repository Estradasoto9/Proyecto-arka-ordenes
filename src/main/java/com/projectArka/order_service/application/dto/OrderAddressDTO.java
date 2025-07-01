package com.projectArka.order_service.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderAddressDTO {
    private String id;

    @NotBlank(message = "Street cannot be blank")
    private String street;

    @NotBlank(message = "Number cannot be blank")
    private String number;

    private String apartment;

    @NotBlank(message = "City cannot be blank")
    private String city;

    @NotBlank(message = "State cannot be blank")
    private String state;

    @NotBlank(message = "Country cannot be blank")
    private String country;

    @NotBlank(message = "Postal code cannot be blank")
    @Pattern(regexp = "^[0-9]{5}(?:-[0-9]{4})?$", message = "Invalid postal code format")
    private String postalCode;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}