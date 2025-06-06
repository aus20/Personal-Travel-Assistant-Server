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
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.LocalDate
import java.time.format.DateTimeParseException

@RestController
@RequestMapping("/api/flights")
class FlightController(
    private val flightDataProvider: FlightDataProvider,
    private val userFlightSearchService: UserFlightSearchService // Bu enjeksiyonun olduğundan emin olun
) {
    // Logger'ı sınıf seviyesinde tanımlayın
    private val logger = LoggerFactory.getLogger(FlightController::class.java)

    //YENİ DATE FORMAT
    // Bu formatlayıcılar ve fonksiyon FlightController'a özel olabilir
    private val clientInputDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
    private val amadeusRequiredDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE // yyyy-MM-dd

    private fun ensureAmadeusDateFormat(dateString: String?): String? {
        if (dateString.isNullOrBlank()) return null
        return try {
            // Önce "yyyy-MM-dd" formatında mı diye bak
            LocalDate.parse(dateString, amadeusRequiredDateFormatter)
            dateString // Zaten doğru formatta
        } catch (e: DateTimeParseException) {
            // Değilse, "MMM d, uuuu" formatını "yyyy-MM-dd" formatına çevirmeyi dene
            try {
                val parsedDate = LocalDate.parse(dateString, clientInputDateFormatter)
                parsedDate.format(amadeusRequiredDateFormatter)
            } catch (e2: DateTimeParseException) {
                logger.error("Controller: Tarih formatı ayrıştırılamadı: '$dateString'", e2)
                throw IllegalArgumentException("Geçersiz tarih formatı: '$dateString'. 'Ay Gün, Yıl' veya 'YYYY-MM-DD' formatında olmalıdır.")
            }
        }
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
        @RequestBody requestDTO: FlightSearchRequestDTO,
        @CurrentUser user: User? // Kullanıcı opsiyonel
    ): ResponseEntity<WrappedFlightSearchResponseDTO> { 
        logger.info("FlightController: /search/advanced called by user: ${user?.email ?: "Anonymous"} with DTO: $requestDTO")
        
        // YENİ DATE FORMAT Flight Controller'a özel 
        // Tarihleri Amadeus'un beklediği formata çevir
        val departureDateForService = ensureAmadeusDateFormat(requestDTO.departureDate)
            ?: throw IllegalArgumentException("Gidiş tarihi (departureDate) boş olamaz veya formatı geçersiz.")
        val returnDateForService = ensureAmadeusDateFormat(requestDTO.returnDate)

        // FlightDataProvider'a göndermek için yeni (veya güncellenmiş) DTO oluştur
        val serviceRequestDTO = requestDTO.copy(
            departureDate = departureDateForService,
            returnDate = returnDateForService
        )
        // Yeni DTO'yu FlightDataProvider'a gönder
        val flightResultsList: List<FlightResponseDTO> = flightDataProvider.searchFlightsWithFilters(serviceRequestDTO)
        logger.info("---- flightResultsList from DataProvider (Size: ${flightResultsList.size}) ----")
        flightResultsList.forEachIndexed { index, flight ->
            logger.info("Flight ${index + 1}: Origin=${flight.origin}, Dest=${flight.destination}, Price=${flight.price}, Leg=${flight.leg}")
        }
        logger.info("---- End of flightResultsList from DataProvider ----")
        
        /* <<<--- KAYDETME ÖZELLİĞİ YORUM SATIRI YAPILDI ---<<<
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
        */ // <<<--- YORUM SATIRI BİTTİ ---<<<
        val wrappedResponse = WrappedFlightSearchResponseDTO(departureFlight = flightResultsList)
        return ResponseEntity.ok(wrappedResponse)
    }
}
