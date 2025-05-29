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
        @RequestBody requestDTO: FlightSearchRequestDTO,
        @CurrentUser user: User? // Kullanıcı opsiyonel
    ): ResponseEntity<WrappedFlightSearchResponseDTO> { 
        logger.info("FlightController: /search/advanced called by user: ${user?.email ?: "Anonymous"} with DTO: $requestDTO")
        val flightResultsList: List<FlightResponseDTO> = flightDataProvider.searchFlightsWithFilters(requestDTO)
        
        if (user != null) {
            // Uçuş sonucu olup olmamasına bakılmaksızın arama kriterlerini kaydetmeyi dene
            // (saveSearchAndInitialFlight metodu boş flightResultsList durumunu da ele alıyor)
            try {
                logger.info("Attempting to save search criteria (and initial flight if any) for user ${user.email}.")
                userFlightSearchService.saveSearchAndInitialFlight(requestDTO, user, flightResultsList)
                // Bu metodun dönüş değeri (UserFlightSearch?) burada kullanılmıyor.
                // Başarılı loglaması UserFlightSearchService içinde yapılıyor.
            } catch (e: IllegalArgumentException) {
                // Genellikle tarih formatı hatası veya beklenen bir durum (örn: arama zaten var ve hata fırlatıyorsa)
                logger.warn("Failed to save search criteria for user ${user.email} (IllegalArgumentException): ${e.message}")
            } catch (e: Exception) {
                // Diğer beklenmedik hatalar
                logger.error("Unexpected error during saving search criteria for user ${user.email}", e)
            }
        } else {
            logger.info("Anonymous search. Search criteria not saved.")
        }
        
        val wrappedResponse = WrappedFlightSearchResponseDTO(departureFlight = flightResultsList)
        return ResponseEntity.ok(wrappedResponse)
    }
}
