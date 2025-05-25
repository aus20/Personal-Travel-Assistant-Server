package com.aus20.dto.response


import java.time.LocalDate

// FlightResponseDTO'yu zaten uçuş arama sonuçları için kullanıyoruz.
// Kaydedilmiş aramanın içindeki uçuşlar için de bunu kullanabiliriz
// ya da daha özet bir FlightSummaryDTO oluşturabiliriz.
// Şimdilik FlightResponseDTO'yu kullanalım.

data class SavedUserSearchResponseDTO(
    val searchId: Long,
    val message: String,
    val origin: String,
    val destination: String,
    val departureDate: String, // Veya LocalDate
    val returnDate: String?,   // Veya LocalDate?
    val maxPrice: Int?,
    val adults: Int,
    val preferredAirlines: List<String>?,
    val savedFlights: List<FlightResponseDTO> // Kaydedilen ilk N uçuş
)