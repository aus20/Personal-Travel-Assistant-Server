package com.aus20.dto.request

import jakarta.validation.constraints.*


data class FlightSearchRequestDTO(
    @NotBlank(message = "Origin is required")
    val origin: String,

    @NotBlank(message = "Destination is required")
    val destination: String,

    @Pattern(
        regexp = "\\d{4}-\\d{2}-\\d{2}",
        message = "Departure date must be in the format YYYY-MM-DD"
    )
    val departureDate: String,

    @Pattern(
        regexp = "\\d{4}-\\d{2}-\\d{2}",
        message = "Return date must be in the format YYYY-MM-DD"
    )
    val returnDate: String? = null,

    val isRoundTrip: Boolean = false,

    @DecimalMin(value = "0.0", inclusive = false, message = "Max price must be greater than 0")
    val maxPrice: Int? = null,

    val preferredAirlines: List<String>? = null,

    @Min(value = 1, message = "At least one adult must be specified")
    val adults: Int = 1
)