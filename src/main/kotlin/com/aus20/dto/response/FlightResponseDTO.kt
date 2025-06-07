package com.aus20.dto.response

import com.aus20.dto.enums.FlightLegType
// this is the response dto for the flight search
// it is used to send the response to the client
data class FlightResponseDTO(
    val origin: String,
    val destination: String,
    val originAirportCode: String,
    val destinationAirportCode: String, 
    val layoverAirports: List<String>, 
    val departureTime: String,
    val arrivalTime: String,
    val carrier: String, 
    val duration: String,
    val aircraftCode: String,
    val cabinClass: String,
    val numberOfStops: Int,
    val price: Double,
    val currency: String,
    val leg: FlightLegType
)
