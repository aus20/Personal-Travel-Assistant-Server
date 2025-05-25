// src/main/kotlin/com/aus20/service/MockFlightService.kt
// Mock uçuş verisi sağlayan servis
package com.aus20.service

import com.aus20.dto.request.FlightSearchRequestDTO
import com.aus20.dto.response.FlightResponseDTO
import com.aus20.dto.response.RoundTripFlightResponseDTO
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random // Rastgele veri için
import com.aus20.dto.enums.FlightLegType
@Service
@Profile("dev") // Sadece "dev" profili aktifken bu bean kullanılır
class MockFlightService : FlightDataProvider {

    private val logger = LoggerFactory.getLogger(MockFlightService::class.java)
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override fun searchFlightsWithFilters(request: FlightSearchRequestDTO): Any {
        logger.info("MockFlightService: searchFlightsWithFilters called with request: $request")

        // İstek DTO'sundaki maxPrice ve preferredAirlines'ı dikkate alabilirsiniz
        // Bu örnekte basit tutuyoruz.
        val departureLegType = if (request.isRoundTrip) FlightLegType.DEPARTURE else FlightLegType.ONE_WAY

        val mockDepartureFlights = generateMockFlights(
            request.origin,
            request.destination,
            request.departureDate,
            request.adults,
            request.maxPrice,
            request.preferredAirlines,
            departureLegType 
        )

        if (request.isRoundTrip && request.returnDate != null) {
            val mockReturnFlights = generateMockFlights(
                request.destination, // Dönüş için tersi
                request.origin,      // Dönüş için tersi
                request.returnDate,
                request.adults,
                request.maxPrice,
                request.preferredAirlines,
                FlightLegType.RETURN
            )
            // Gidiş-dönüş sonucunu döndür
            return RoundTripFlightResponseDTO(
                departureFlight = mockDepartureFlights,
                returnFlight = mockReturnFlights
            )
        }

        // Sadece gidiş sonucunu döndür
        return mockDepartureFlights
    }

    override fun getRawFlightResponse(origin: String, destination: String): Any? {
        logger.info("MockFlightService: getRawFlightResponse called for $origin -> $destination")
        // Basit bir mock yanıt
        return mapOf(
            "mockData" to true,
            "message" to "This is a mock raw response",
            "parameters" to mapOf("origin" to origin, "destination" to destination)
        )
    }

    /**
     * Belirtilen kriterlere göre rastgele mock uçuşlar üreten yardımcı fonksiyon.
     */
    private fun generateMockFlights(
        origin: String,
        destination: String,
        date: String,
        adults: Int,
        maxPrice: Int?,
        preferredAirlines: List<String>?,
        legType: FlightLegType,
        count: Int = Random.nextInt(3, 8) // Rastgele 3-7 uçuş üretelim
    ): List<FlightResponseDTO> {

        val airlines = preferredAirlines?.takeIf { it.isNotEmpty() } ?: listOf("TK", "PC", "XQ", "MOCK")

        return (1..count).mapNotNull { _ ->
            val stops = Random.nextInt(0, 3) // 0, 1 veya 2 aktarma
            val basePrice = Random.nextDouble(50.0, 400.0) * adults
            val finalPrice = basePrice + (stops * Random.nextDouble(10.0, 50.0)) // Aktarmalar fiyatı artırsın
            val currency = "USD" // Veya TRY
            val airline = airlines.random() // Rastgele bir havayolu seç

            // Eğer maxPrice varsa ve üretilen fiyat bunu aşıyorsa, bu uçuşu dahil etme (null döndür)
            if (maxPrice != null && finalPrice > maxPrice) {
                null
            } else {
                val departureHour = Random.nextInt(6, 20) // 06:00 - 19:00 arası kalkış
                val durationHours = Random.nextInt(1, 5)
                val durationMinutes = Random.nextInt(0, 60)
                val departureTime = LocalDateTime.parse("${date}T${String.format("%02d", departureHour)}:${String.format("%02d", Random.nextInt(0,60))}:00")
                val arrivalTime = departureTime.plusHours(durationHours.toLong()).plusMinutes(durationMinutes.toLong())

                FlightResponseDTO(
                    origin = origin,
                    destination = destination,
                    departureTime = departureTime.format(formatter),
                    arrivalTime = arrivalTime.format(formatter),
                    carrier = "$airline ${Random.nextInt(100, 9999)}",
                    duration = "PT${durationHours}H${durationMinutes}M",
                    aircraftCode = listOf("738", "321", "77W", "359").random(),
                    cabinClass = listOf("ECONOMY", "BUSINESS").random(),
                    numberOfStops = stops,
                    price = finalPrice, // Fiyatı 2 ondalığa yuvarla
                    currency = currency,
                    leg = legType
                )
            }
        }.sortedBy { it.price } // Fiyata göre sıralayalım
    }
}