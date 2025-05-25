package com.aus20.dto.response

// FlightResponseDTO import'u
import com.aus20.dto.response.FlightResponseDTO
// Gerekirse LocalDate/LocalDateTime import'ları

data class UserSearchDetailDTO( // Yeni isim önerisi
    val id: Long,
    val origin: String,
    val destination: String,
    val departureDate: String,
    val returnDate: String?,
    val isRoundTrip: Boolean,
    val maxPrice: Int?,
    val adults: Int,
    val preferredAirlines: List<String>?,
    val createdAt: String,
    val flights: List<FlightResponseDTO> // Bu arama için kaydedilmiş uçuşlar
)