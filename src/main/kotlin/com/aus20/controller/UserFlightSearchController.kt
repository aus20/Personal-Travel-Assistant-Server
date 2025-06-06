package com.aus20.controller

import com.aus20.dto.request.FlightSearchRequestDTO
import com.aus20.domain.User
import com.aus20.domain.UserFlightSearch
import com.aus20.security.CurrentUser
import com.aus20.service.UserFlightSearchService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.security.core.annotation.AuthenticationPrincipal
import com.aus20.dto.response.SavedUserSearchResponseDTO
import com.aus20.dto.response.UserSearchDetailDTO
import com.aus20.dto.response.SimpleSavedSearchResponseDTO
import com.aus20.service.FlightDataProvider // <<<--- YENİ IMPORT
import org.slf4j.LoggerFactory
import com.aus20.dto.response.FlightResponseDTO


@RestController
@RequestMapping("/api/user-searches")
class UserFlightSearchController(
    private val userFlightSearchService: UserFlightSearchService,
    private val flightDataProvider: FlightDataProvider
) {
    private val logger = LoggerFactory.getLogger(UserFlightSearchController::class.java)
      
    @PostMapping
    fun saveUserSearch(
        @RequestBody request: FlightSearchRequestDTO,
        @CurrentUser user: User
    ): ResponseEntity<SimpleSavedSearchResponseDTO> { // <<<--- DÖNÜŞ TİPİ DEĞİŞTİ
        logger.info("User ${user.email} is explicitly saving a search with DTO: $request")
        try {
            // 1. Bu arama için güncel uçuşları FlightDataProvider'dan çek
            val currentFetchedFlights: List<FlightResponseDTO> = flightDataProvider.searchFlightsWithFilters(request)
            logger.info("---- UserFlightSearchController: currentFetchedFlights for saving (Size: ${currentFetchedFlights.size}) ----")
            currentFetchedFlights.forEachIndexed { index, flight ->
                logger.info("SavingContext Flight ${index + 1}: Origin=${flight.origin}, Dest=${flight.destination}, Price=${flight.price}, Leg=${flight.leg}")
            }
            logger.info("---- End of currentFetchedFlights for saving ----")

            // 2. Arama kriterlerini ve (varsa) en ucuz uçuşu kaydetmek/güncellemek için servisi çağır.
            // Bu metot artık SimpleSavedSearchResponseDTO döndürüyor.
            val savedSearchResponseDto = userFlightSearchService.saveSearchAndInitialFlight(request, user, currentFetchedFlights)
            
            return ResponseEntity.ok(savedSearchResponseDto)

        } catch (e: IllegalArgumentException) {
            // Genellikle tarih formatı hatası veya UserFlightSearchService'ten fırlatılan diğer beklenen hatalar
            logger.warn("User ${user.email}: Failed to save search (IllegalArgumentException): ${e.message} for DTO: $request")
            // GlobalExceptionHandler bu hatayı yakalayıp uygun bir HTTP 400 yanıtı oluşturacaktır.
            throw e 
        } catch (e: Exception) {
            logger.error("User ${user.email}: Unexpected error while saving search for DTO: $request", e)
            // Genel bir hata DTO'su veya sadece HTTP 500 durumu dönülebilir.
            // İsterseniz burada da SimpleSavedSearchResponseDTO içinde bir hata mesajı dönebilirsiniz.
            // Örneğin:
            // val errorResponse = SimpleSavedSearchResponseDTO(searchId = -1, message = "Error saving search: ${e.localizedMessage}", ...)
            // return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
            return ResponseEntity.internalServerError().build() 
        }
    }
    
    
    /* 
    @GetMapping
    fun getUserSearches(
        @CurrentUser user: User
    ): ResponseEntity<List<UserSearchDetailDTO>> {
        val searches = userFlightSearchService.getUserSearches(user)
        return ResponseEntity.ok(searches) // Servisten gelen DTO listesini direkt döndür
    }
    */
    /* 
    @PutMapping("/{searchId}")
    fun updateFlightSearch(
        @PathVariable searchId: Long,
        @RequestBody dto: FlightSearchRequestDTO,
        @CurrentUser user: User
    ): ResponseEntity<UserSearchDetailDTO> {
        val updatedSearch = userFlightSearchService.updateUserSearch(searchId, dto, user)
        return ResponseEntity.ok(updatedSearch)
    }
    */
    @DeleteMapping("/{searchId}")
    fun deleteFlightSearch(
        @PathVariable searchId: Long,
        @CurrentUser user: User
    ): ResponseEntity<Map<String, String>> {
        userFlightSearchService.deleteUserSearch(searchId, user)
        return ResponseEntity.ok(mapOf("message" to "Flight search with ID $searchId successfully deleted."))
    }
}
