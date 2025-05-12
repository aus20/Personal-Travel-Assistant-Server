package com.aus20.controller

import com.aus20.domain.Flight
import com.aus20.service.FlightService
import com.aus20.dto.response.FlightResponseDTO
import org.springframework.http.ResponseEntity
import com.aus20.dto.request.FlightSearchRequestDTO
import com.aus20.service.FlightDataProvider
import org.springframework.http.HttpStatus // (eski searchFlights yanıtı için)
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/flights")
class FlightController(
    private val flightService: FlightService,
    private val flightDataProvider: FlightDataProvider
) {

    @GetMapping("/search")
    fun searchFlights(
        @RequestParam origin: String,
        @RequestParam destination: String,
        // şimdilik kalsın sonrasında daha belirli variable'lar kullanılacak.
    ): ResponseEntity<List<FlightResponseDTO>> {
        val flights = flightService.searchFlights(origin, destination)
        val response = flights.map {
            FlightResponseDTO(
                origin = it.origin,
                destination = it.destination,
                departureTime = it.departureTime.toString(),
                arrivalTime = it.arrivalTime.toString(),
                carrier = "${it.carrierCode} ${it.flightNumber}",
                duration = it.duration,
                aircraftCode = it.aircraftCode,
                cabinClass = it.cabinClass,
                numberOfStops = it.numberOfStops,
                price = it.price,
                currency = it.currency
            )
        }
        return ResponseEntity.ok(response)
    }
    @GetMapping("/raw")
    fun getRawFlights(
        @RequestParam origin: String,
        @RequestParam destination: String
        // şimdilik kalsın sonrasında daha belirli variable'lar kullanılacak.
    ): ResponseEntity<Any?> {
        val rawData = flightDataProvider.getRawFlightResponse(origin, destination)
        return ResponseEntity.ok(rawData)
    }
    @PostMapping("/search/advanced")
    fun searchAdvancedFlights(
        @RequestBody requestDTO: FlightSearchRequestDTO
    ): ResponseEntity<Any> {
        val result = flightDataProvider.searchFlightsWithFilters(requestDTO)
        return ResponseEntity.ok(result)
    }
}
