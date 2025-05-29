// src/main/kotlin/com/aus20/service/FlightDataProvider.kt
// Bu arayüz, gerçek API çağrıları yapan veya mock data döndüren
// farklı implementasyonlar arasında geçiş yapmayı kolaylaştırır.

package com.aus20.service

import com.aus20.dto.request.FlightSearchRequestDTO
import com.aus20.dto.response.FlightResponseDTO

/**
 * Uçuş verisi sağlayan servisler için ortak arayüz.
 * Bu arayüz, gerçek API çağrıları yapan veya mock data döndüren
 * farklı implementasyonlar arasında geçiş yapmayı kolaylaştırır.
 */
interface FlightDataProvider {

    /**
     * Uçuş arama isteklerini alır ve filtrelere göre uçuşları döndürür.
     * @param request DTO içindeki filtreler ve arama kriterleri kullanılarak uçuşlar aranır.
     * @return Filtrelere göre bulunan uçuşların listesi.
     */
    fun searchFlightsWithFilters(request: FlightSearchRequestDTO): List<FlightResponseDTO>

    /**
     * Basit arama için ham (raw) API yanıtını döndürür.
     * (Bu metodun mock implementasyonu basit bir Map döndürebilir)
     */
    fun getRawFlightResponse(origin: String, destination: String): Any?

    // Not: Mevcut FlightService'te başka public metodlar varsa
    // (örneğin eski searchFlights metodu), ve onlara da ihtiyaç duyuluyorsa
    // buraya ekleyebilirsiniz. Ancak genellikle ana arama metodu yeterli olur.
}