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

@RestController
@RequestMapping("/api/user-searches")
class UserFlightSearchController(
    private val userFlightSearchService: UserFlightSearchService
) {
    /*  
    @PostMapping
    fun saveUserSearch(
        @RequestBody request: FlightSearchRequestDTO,
        @CurrentUser user: User
    ): ResponseEntity<SimpleSavedSearchResponseDTO> { // <<<--- DÖNÜŞ TİPİ DEĞİŞTİ
        val savedSearchDetails = userFlightSearchService.saveSearchWithTopFlights(request, user)
        return ResponseEntity.ok(savedSearchDetails) // Direkt servisten gelen DTO'yu döndür
    }
    */

    @GetMapping
    fun getUserSearches(
        @CurrentUser user: User
    ): ResponseEntity<List<UserSearchDetailDTO>> {
        val searches = userFlightSearchService.getUserSearches(user)
        return ResponseEntity.ok(searches) // Servisten gelen DTO listesini direkt döndür
    }
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
