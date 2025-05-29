package com.aus20.controller

import com.aus20.domain.Flight
import com.aus20.service.FlightService
import com.aus20.dto.response.FlightResponseDTO
import org.springframework.http.ResponseEntity
import com.aus20.dto.request.FlightSearchRequestDTO
import com.aus20.service.FlightDataProvider
import com.aus20.dto.response.WrappedFlightSearchResponseDTO
import org.springframework.http.HttpStatus // Eklendi (eski searchFlights yanıtı için)
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
// CurrentUser anotasyonu ve User sınıfı için gerekli importlar
import com.aus20.domain.User
import com.aus20.security.CurrentUser
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.DeleteMapping
import org.slf4j.LoggerFactory
import org.slf4j.Logger
import com.aus20.service.UserFlightSearchService

@RestController
@RequestMapping("/api/flights")
class FlightController(
    private val flightDataProvider: FlightDataProvider,
    private val userFlightSearchService: UserFlightSearchService // Bu enjeksiyonun olduğundan emin olun
) {
    // Logger'ı sınıf seviyesinde tanımlayın
    private val logger = LoggerFactory.getLogger(FlightController::class.java)

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
        //@CurrentUser user: User? // Kullanıcı opsiyonel
    ): ResponseEntity<WrappedFlightSearchResponseDTO> { 

        val flightResultsList: List<FlightResponseDTO> = flightDataProvider.searchFlightsWithFilters(requestDTO)

        val wrappedResponse = WrappedFlightSearchResponseDTO(departureFlight = flightResultsList)
        return ResponseEntity.ok(wrappedResponse)
    }
}
