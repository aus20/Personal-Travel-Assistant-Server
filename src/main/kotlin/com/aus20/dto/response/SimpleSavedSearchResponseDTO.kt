package com.aus20.dto.response

// Sadece temel arama bilgilerini ve başarı mesajını içeren DTO
data class SimpleSavedSearchResponseDTO(
    val searchId: Long,
    val message: String,
    val origin: String,
    val destination: String,
    val departureDate: String, // Veya LocalDate
    val returnDate: String?,   // Veya LocalDate?
    val adults: Int
)