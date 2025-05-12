package com.aus20.dto.response

data class RoundTripFlightResponseDTO(
    val departureFlight: List<FlightResponseDTO>,
    val returnFlight: List<FlightResponseDTO>
)
