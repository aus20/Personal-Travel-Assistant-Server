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
import java.util.Locale
import java.time.LocalDate
import java.time.format.DateTimeParseException

@Service
@Profile("dev") // Sadece "dev" profili aktifken bu bean kullanılır
class MockFlightService : FlightDataProvider {

    private val logger = LoggerFactory.getLogger(MockFlightService::class.java)
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    // <<<--- YENİ: Tarih çevrim fonksiyonları ve formatter'lar ---<<<
    // FlightService'teki ile aynı formatter'ları kullanın
    private val inputDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
    private val amadeusDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE // YYYY-MM-DD

    private fun convertToAmadeusDate(dateString: String?): String? {
        if (dateString.isNullOrBlank()) {
            return null
        }
        return try {
            val parsedDate = LocalDate.parse(dateString, inputDateFormatter)
            parsedDate.format(amadeusDateFormatter)
        } catch (e: DateTimeParseException) {
            try {
                LocalDate.parse(dateString, amadeusDateFormatter) // Zaten doğru formatta mı diye kontrol et
                dateString // Doğru formatta ise olduğu gibi döndür
            } catch (e2: DateTimeParseException) {
                logger.error("MockService: Tarih formatı ayrıştırılamadı: '$dateString'. Beklenen formatlar: 'MMM d,คณะกรรมการ' veya 'yyyy-MM-dd'", e)
                throw IllegalArgumentException("Geçersiz tarih formatı: '$dateString'. 'Ay Gün, Yıl' (örn: 'May 30, 2025') veya 'YYYY-MM-DD' formatında olmalıdır.")
            }
        }
    }
    // <<<--- BİTTİ ---<<<

    private companion object {
        const val TOTAL_FLIGHTS_TO_DISPLAY = 20 // Gösterilecek toplam uçuş sayısı
    }

    override fun searchFlightsWithFilters(request: FlightSearchRequestDTO): List<FlightResponseDTO> {
        logger.info("MockFlightService: searchFlightsWithFilters called with request: $request")

        // İstek DTO'sundaki maxPrice ve preferredAirlines'ı dikkate alabilirsiniz
        // Bu örnekte basit tutuyoruz.
        // <<<--- YENİ: Tarih formatı çevrimi ---<<<
        val convertedDepartureDate: String
        try {
            convertedDepartureDate = convertToAmadeusDate(request.departureDate)
                ?: throw IllegalArgumentException("Gidiş tarihi (departureDate) boş olamaz veya formatı geçersiz.")
        } catch (e: IllegalArgumentException) {
            logger.warn("MockService: Departure date conversion failed: ${e.message}")
            throw e // İstisnayı tekrar fırlat ki GlobalExceptionHandler yakalasın
        }

        val convertedReturnDate: String? = try {
            convertToAmadeusDate(request.returnDate)
        } catch (e: IllegalArgumentException) {
            logger.warn("MockService: Return date conversion failed: ${e.message}")
            throw e
        }
        // <<<--- BİTTİ ---<<<

        val isRoundTripSearch = convertedReturnDate != null

        if (!isRoundTripSearch) {
            // Tek yön arama
            val oneWayResults = generateMockFlights(
                request.origin,
                request.destination,
                convertedDepartureDate,
                request.adults,
                request.maxPrice,
                request.preferredAirlines,
                FlightLegType.ONE_WAY,
                count = Random.nextInt(15, 30) // Rastgele 15-30 arası uçuş üretiyoruz
            ).sortedBy { it.price } // Fiyata göre sıralayalım
            return oneWayResults.take(TOTAL_FLIGHTS_TO_DISPLAY) // İstenilen sayıda uçuş döndür
        } else {
            // Gidiş-dönüş arama
        val departureFlights = generateMockFlights(
            request.origin,
            request.destination,
            convertedDepartureDate,
            request.adults,
            request.maxPrice,
            request.preferredAirlines,
            FlightLegType.DEPARTURE,
            count = Random.nextInt(15, 30) // Rastgele 15-30 arası uçuş üretiyoruz
        ).sortedBy { it.price }
            val returnFlights = generateMockFlights(
                request.destination,
                request.origin,
                convertedReturnDate!!,
                request.adults,
                request.maxPrice,
                request.preferredAirlines,
                FlightLegType.RETURN,
                count = Random.nextInt(15, 30) // Rastgele 15-30 arası uçuş üretiyoruz
            ).sortedBy { it.price }

        // İki listeyi istenen şekilde birleştir
            val mergedList = mutableListOf<FlightResponseDTO>()
            val depIterator = departureFlights.iterator()
            val retIterator = returnFlights.iterator()

            while (depIterator.hasNext() || retIterator.hasNext()) {
                if (depIterator.hasNext()) {
                    mergedList.add(depIterator.next())
                }
                if (retIterator.hasNext()) {
                    mergedList.add(retIterator.next())
                }
            }
            return mergedList.take(TOTAL_FLIGHTS_TO_DISPLAY)
            
        }
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
        count: Int 
    ): List<FlightResponseDTO> {

        val airlines = preferredAirlines?.takeIf { it.isNotEmpty() } ?: listOf("TK", "PC", "XQ", "MOCK")

        return (1..count).mapNotNull { _ ->
            val stops = Random.nextInt(0, 2) // 0 veya 1 aktarma
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

                // Sahte havaalanı kodları oluştur (Şehrin ilk 3 harfi + M/K)
                val mockOriginAirport = origin.take(3).uppercase() + "M"
                val mockDestinationAirport = destination.take(3).uppercase() + "K"

                // <<<--- YENİ: Sahte aktarma listesi oluştur ---<<<
                val layovers = if (stops > 0) {
                    (1..stops).map {
                        listOf("MUC", "FRA", "AMS", "CDG", "ZRH", "VIE").random() // Rastgele aktarma kodları
                    }
                } else {
                    emptyList<String>()
                }
                // <<<--- BİTTİ ---<<<

                FlightResponseDTO(
                    origin = origin,
                    destination = destination,
                    originAirportCode = mockOriginAirport,
                    destinationAirportCode = mockDestinationAirport,
                    layoverAirports = layovers, // Aktarma havaalanı kodları
                    departureTime = departureTime.format(formatter),
                    arrivalTime = arrivalTime.format(formatter),
                    carrier = "$airline ${Random.nextInt(100, 9999)}",
                    duration = "${durationHours}H${durationMinutes}M",
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