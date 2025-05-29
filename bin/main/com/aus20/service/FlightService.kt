package com.aus20.service

import com.aus20.domain.Flight
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.http.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.aus20.dto.request.FlightSearchRequestDTO
import com.aus20.dto.response.RoundTripFlightResponseDTO
import com.aus20.dto.response.FlightResponseDTO
import org.springframework.web.client.HttpClientErrorException
import org.springframework.context.annotation.Profile // Profile import'u
import com.aus20.dto.enums.FlightLegType
import org.slf4j.LoggerFactory
import org.springframework.web.util.UriComponentsBuilder // URL oluşturmak için kullanılır
import java.time.Duration
import java.time.format.DateTimeParseException
import java.util.Locale
import java.time.LocalDate

@Service
@Profile("!dev")  // Bu profildeki bean'ler sadece "dev" profili aktif değilse kullanılır.
class FlightService(
    private val authService: AmadeusAuthService,
    private val restTemplate: RestTemplate
) : FlightDataProvider {
    private val logger = LoggerFactory.getLogger(FlightService::class.java)

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
            logger.error("Tarih formatı ayrıştırılamadı: '$dateString'. Beklenen format: 'MMM d, yyyy'", e)
            // Hata durumunda null dönebilir veya bir istisna fırlatabilirsiniz.
            // İstisna fırlatmak, istemciye geçersiz format hakkında bilgi verir.
            throw IllegalArgumentException("Geçersiz tarih formatı: '$dateString'. 'Ay Gün, Yıl' (örn: 'May 30, 2025') formatında olmalıdır.")
        }
    }

    /* 
    // <<<--- YENİ SABİTLER ---<<<
    private companion object {
        const val MAX_FLIGHT_OFFERS_ONE_WAY = 20
        const val MAX_FLIGHT_OFFERS_PER_LEG_ROUND_TRIP = 10
    }
    */
    // <<<--- YENİ SABİT ---<<<
    private companion object {
        const val TOTAL_FLIGHTS_TO_DISPLAY = 20 // Gösterilecek toplam uçuş sayısı
    }

    // MockFlightService'deki getRawFlightResponse'ın aynısı
    override fun getRawFlightResponse(origin: String, destination: String): Any? {
        val token = authService.getAccessToken()
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            setBearerAuth(token)
        }
        val url = "https://test.api.amadeus.com/v2/shopping/flight-offers" +
            "?originLocationCode=$origin&destinationLocationCode=$destination" +
            "&departureDate=2025-05-01&adults=1"
        
        val response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            HttpEntity<String>(headers),
            Map::class.java
        )
        return response.body
    }

    // MockFlightService'deki searchFlightsWithFilters'ın aynısı
    override fun searchFlightsWithFilters(request: FlightSearchRequestDTO): List<FlightResponseDTO> {
        logger.info("FlightService: searchFlightsWithFilters called with request: $request") // Gelen isteği logla
        
        // <<--- YENİ: Tarih formatı çevrimi ve validasyon ---<<<
    val convertedDepartureDate: String
    try {
        convertedDepartureDate = convertToAmadeusDate(request.departureDate)
            ?: throw IllegalArgumentException("Gidiş tarihi (departureDate) boş olamaz veya formatı geçersiz.")
    } catch (e: IllegalArgumentException) {
        logger.warn("Departure date conversion failed: ${e.message}")
        // Burada istemciye 400 Bad Request döndürmek daha uygun olabilir.
        // Bu, GlobalExceptionHandler tarafından yakalanabilir veya burada özel bir ResponseEntity dönebilirsiniz.
        // Şimdilik, bir istisna fırlatarak GlobalExceptionHandler'ın devralmasını bekleyelim.
        throw e
    }

    val convertedReturnDate: String? = try {
        convertToAmadeusDate(request.returnDate)
    } catch (e: IllegalArgumentException) {
        logger.warn("Return date conversion failed: ${e.message}")
        throw e // Aynı şekilde istisna fırlat
    }
    // <<<--- BİTTİ ---<<<
        
        
        val token = authService.getAccessToken()
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            setBearerAuth(token)
        }

        val isRoundTripSearch = convertedReturnDate != null
        // Gidiş ayağı için legType belirle (tek yön ise ONE_WAY, gidiş-dönüş ise DEPARTURE)

        /* 
        val departureLegType = if (isRoundTripSearch) {
            FlightLegType.DEPARTURE
        } else {
            FlightLegType.ONE_WAY
        }

        val departureFlights = fetchFlights(
            origin = request.origin,
            destination = request.destination,
            date = request.departureDate,
            adults = request.adults,
            preferredAirlines = request.preferredAirlines,
            maxPrice = request.maxPrice,
            headers = headers,
            legType = departureLegType
        )

        if (isRoundTripSearch && request.returnDate != null) {
            val returnFlights = fetchFlights(
                origin = request.destination,
                destination = request.origin,
                date = request.returnDate,
                adults = request.adults,
                preferredAirlines = request.preferredAirlines,
                maxPrice = request.maxPrice,
                headers = headers,
                legType = FlightLegType.RETURN
            )
            return RoundTripFlightResponseDTO(
                departureFlight = departureFlights,
                returnFlight = returnFlights
            )
        }

        return departureFlights
    }
    */
        if (!isRoundTripSearch) {
            // Tek Yön Arama
            return fetchFlights(
                origin = request.origin,
                destination = request.destination,
                date = convertedDepartureDate, // Amadeus formatına çevrilmiş tarih
                adults = request.adults,
                // maxPrice ve preferredAirlines DTO'dan kaldırıldığı için null veya boş liste geçilebilir
                // Eğer fetchFlights bunları hala bekliyorsa, imzası güncellenmeli.
                // Son konuşmamıza göre DTO'dan kaldırılmışlardı.
                preferredAirlines = request.preferredAirlines,
                maxPrice = request.maxPrice,
                headers = headers,
                legType = FlightLegType.ONE_WAY,
                //maxResults = MAX_FLIGHT_OFFERS_ONE_WAY // Tek yön uçuşlar için maksimum sonuç sayısı
            ).sortedBy { it.price } // Tek yön uçuşları da fiyata göre sıralayalım
        } else 
        {
            // Gidiş-Dönüş Arama

            // Gidiş uçuşlarını al ve fiyata göre sırala
            val departureFlights = fetchFlights(
                origin = request.origin,
                destination = request.destination,
                date = convertedDepartureDate,
                adults = request.adults,
                preferredAirlines = request.preferredAirlines,
                maxPrice = request.maxPrice,
                headers = headers,
                legType = FlightLegType.DEPARTURE
                //maxResults = MAX_FLIGHT_OFFERS_PER_LEG_ROUND_TRIP
            ).sortedBy { it.price }

            // Dönüş uçuşlarını al ve fiyata göre sırala (request.returnDate null olamaz)
            val returnFlights = fetchFlights(
                origin = request.destination,
                destination = request.origin,
                date = convertedReturnDate!!, // returnDate'in null olmadığından eminiz (isRoundTripSearch kontrolü)
                adults = request.adults,
                preferredAirlines = request.preferredAirlines,
                maxPrice = request.maxPrice,
                headers = headers,
                legType = FlightLegType.RETURN
                //maxResults = MAX_FLIGHT_OFFERS_PER_LEG_ROUND_TRIP
            ).sortedBy { it.price }

            // İki listeyi istenen şekilde birleştir
            val mergedList = mutableListOf<FlightResponseDTO>()
            val depIterator = departureFlights.iterator()
            val retIterator = returnFlights.iterator()

            // En ucuz gidiş, en ucuz dönüş şeklinde sırayla ekle
            while (depIterator.hasNext() || retIterator.hasNext()) {
                if (depIterator.hasNext()) {
                    mergedList.add(depIterator.next())
                }
                if (retIterator.hasNext()) {
                    mergedList.add(retIterator.next())
                }
            }
            return mergedList.take(TOTAL_FLIGHTS_TO_DISPLAY) // Toplam uçuş sayısını sınırlıyoruz
        }
    }


    private fun fetchFlights(
        origin: String,
        destination: String,
        date: String,
        adults: Int,
        preferredAirlines: List<String>?,
        maxPrice: Int?,
        headers: HttpHeaders,
        legType: FlightLegType
    ): List<FlightResponseDTO> {

        // Şehir kodlarını al
        val originCode = getCityCode(origin)
        val destinationCode = getCityCode(destination)
        // BİTTİ

        val baseUrl = "https://test.api.amadeus.com/v2/shopping/flight-offers"
        val urlBuilder = StringBuilder("$baseUrl?originLocationCode=$originCode&destinationLocationCode=$destinationCode&departureDate=$date&adults=$adults")

        preferredAirlines?.takeIf { it.isNotEmpty() }?.let {
            val airlinesParam = it.joinToString(",")
            urlBuilder.append("&includedAirlineCodes=$airlinesParam")
        }

        maxPrice?.let {
            urlBuilder.append("&maxPrice=$it")
        }
        
        val response = try { 
            restTemplate.exchange(
                urlBuilder.toString(),
                HttpMethod.GET,
                HttpEntity<String>(headers),
                Map::class.java
            )
        } catch (e: HttpClientErrorException.TooManyRequests) {
            logger.warn("Too many requests: ${e.statusCode} - ${e.responseBodyAsString}") //looger.warn
            return emptyList() // or throw a custom exception, or add retry logic
        } catch (e: HttpClientErrorException) {
            logger.error("API error: ${e.statusCode} - ${e.responseBodyAsString}") //logger.error
            return emptyList()
        }

        val data = response.body?.get("data") as? List<*> ?: return emptyList()

        return data.mapNotNull { offerRaw ->
            val offer = offerRaw as? Map<String, Any> ?: return@mapNotNull null
            try {
                val itineraries = (offer["itineraries"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList()
                // Eğer itineraries boşsa, bu uçuşu atla
                if (itineraries.isNullOrEmpty()) return@mapNotNull null

                val firstItinerary = itineraries.first()
                val segments = (firstItinerary["segments"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList()
                // Eğer segmentler boşsa, bu uçuşu atla
                
                if (segments.isNullOrEmpty()) return@mapNotNull null

                val firstSegment = segments.first()
                val lastSegment = segments.last()

                val departure = firstSegment["departure"] as? Map<String, Any> ?: return@mapNotNull null
                val arrival = lastSegment["arrival"] as? Map<String, Any> ?: return@mapNotNull null

                // *** YENİ: Havaalanı kodlarını ayıkla ***
                val originAirport = departure["iataCode"] as? String ?: ""
                val destinationAirport = arrival["iataCode"] as? String ?: ""
                // *** BİTTİ ***

                val overallDepartureTimeRaw = departure["at"] as? String 
                val overallArrivalTimeRaw = arrival["at"] as? String  

                val formatter = DateTimeFormatter.ISO_DATE_TIME

                val departureTime = overallDepartureTimeRaw?.let { LocalDateTime.parse(it, formatter) } ?: ""
                val arrivalTime = overallArrivalTimeRaw?.let { LocalDateTime.parse(it, formatter) } ?: ""

                // *** YENİ: Aktarma havaalanı kodlarını ayıkla ***
                val layovers = if (segments.size > 1) {
                    // Eğer 1'den fazla segment varsa, ilk segmentten sondan bir önceki segmente kadar
                    // tüm segmentlerin 'varış' havaalanı kodlarını al.
                    segments.subList(0, segments.size - 1).mapNotNull { segment ->
                        (segment["arrival"] as? Map<String, Any>)?.get("iataCode") as? String
                    }
                } else {
                    // Aktarma yoksa boş liste döndür.
                    emptyList<String>()
                }
                // *** BİTTİ ***

                val carrierCode = firstSegment["carrierCode"] as? String ?: ""
                val flightNumber = firstSegment["number"] as? String ?: ""
                val aircraftCode = (firstSegment["aircraft"] as? Map<*, *>)?.get("code") as? String ?: "N/A"
                val durationIso = firstItinerary["duration"] as? String ?: "N/A"
                val numberOfStops = segments.size - 1

                val priceBlock = offer["price"] as? Map<*, *> 
                val price = (priceBlock?.get("total") as? String)?.toDoubleOrNull() ?: 0.0
                val currency = priceBlock?.get("currency") as? String ?: "USD"

                val travelerPricings = offer["travelerPricings"] as? List<*> 
                val firstTraveler = travelerPricings?.firstOrNull() as? Map<*, *> 
                val fareDetails = firstTraveler?.get("fareDetailsBySegment") as? List<*> 
                val firstFare = fareDetails?.firstOrNull() as? Map<*, *> 
                val cabinClass = firstFare?.get("cabin") as? String ?: "Unknown"

                FlightResponseDTO(
                    origin = origin,
                    destination = destination,
                    originAirportCode = originAirport,
                    destinationAirportCode = destinationAirport,
                    layoverAirports = layovers,
                    departureTime = departureTime.toString(),
                    arrivalTime = arrivalTime.toString(),
                    carrier = "$carrierCode $flightNumber",
                    duration = formatDuration(durationIso),
                    aircraftCode = aircraftCode,
                    cabinClass = cabinClass,
                    numberOfStops = numberOfStops,
                    price = price,
                    currency = currency,
                    leg = legType
                )
            } catch (e: Exception) {
                println("Mapping failed for offer: ${offer["id"] ?: "unknown"} -> ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }
    // <<<--- YENİ: Popüler Şehirler için Yerel Önbellek ---<<<
    private val cityCodeCache: Map<String, String> = mapOf(
        "istanbul" to "IST",
        "london" to "LON",
        "paris" to "PAR",
        "new york" to "NYC",
        "tokyo" to "TYO",
        "dubai" to "DXB",
        "amsterdam" to "AMS",
        "frankfurt" to "FRA",
        "madrid" to "MAD",
        "barcelona" to "BCN",
        "rome" to "ROM",
        "munich" to "MUC",
        "berlin" to "BER",
        "ankara" to "ANK",
        "izmir" to "IZM",
        "antalya" to "AYT",
        "vienna" to "VIE",
        "brussels" to "BRU",
        "copenhagen" to "CPH",
        "lisbon" to "LIS",
        "dublin" to "DUB",
        "oslo" to "OSL",
        "stockholm" to "STO", 
        "warsaw" to "WAW",
        "prague" to "PRG",
        "budapest" to "BUD",
        "athens" to "ATH",
        "zurich" to "ZRH",
        "geneva" to "GVA",

        
        "los angeles" to "LAX",
        "chicago" to "CHI",
        "toronto" to "YTO",
        "san francisco" to "SFO",
        "miami" to "MIA",

        
        "singapore" to "SIN",
        "hong kong" to "HKG",
        "seoul" to "SEL",
        "bangkok" to "BKK",
        "kuala lumpur" to "KUL",
        "beijing" to "BJS",

        
        "adana" to "ADA",
        "trabzon" to "TZX",
        "gaziantep" to "GZT",
        "bodrum" to "BJV"

    )
    // <<<--- BİTTİ ---<<<
    private fun getCityCode(cityName: String): String {

        val normalizedCityName = cityName.trim().lowercase()

        // 1. Yerel önbelleği kontrol et
        cityCodeCache[normalizedCityName]?.let { iataCode ->
            logger.info("'$cityName' için şehir kodu yerel önbellekte bulundu: '$iataCode'")
            return iataCode
        }
        // 2. Önbellekte yoksa Amadeus API'sini çağır
        logger.info("'$cityName' yerel önbellekte bulunamadı. Amadeus API çağrılıyor...")

        val token = authService.getAccessToken()
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            setBearerAuth(token)
        }

        // Amadeus API'sine gönderilecek URL'yi güvenli bir şekilde oluştur
        val url = UriComponentsBuilder.fromHttpUrl("https://test.api.amadeus.com/v1/reference-data/locations")
            .queryParam("subType", "CITY")
            .queryParam("keyword", cityName)
            .queryParam("page[limit]", 1) // Genellikle en alakalı ilk sonuç yeterlidir
            .queryParam("sort", "analytics.travelers.score") // Alaka düzeyine göre sırala
            .build()
            .toUriString()

        logger.info("'$cityName' için şehir kodu alınıyor: $url")

        return try {
            val response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                HttpEntity<String>(headers),
                Map::class.java // Basitlik için Map kullanıyoruz, daha sonra DTO oluşturulabilir
            )

            // Yanıtı Map olarak alıp 'data' kısmını ayıkla
            val responseBody = response.body as? Map<String, Any>
            val data = responseBody?.get("data") as? List<Map<String, Any>>

            if (data.isNullOrEmpty()) {
                throw IllegalArgumentException("'$cityName' şehri için konum verisi bulunamadı.")
            }

            // 'subType' değeri 'CITY' olan ilk girişi bul, bulamazsan ilk gelen girişi al (fallback)
            val cityEntry = data.firstOrNull { it["subType"] == "CITY" }
                         ?: data.firstOrNull()

            val iataCode = cityEntry?.get("iataCode") as? String

            if (!iataCode.isNullOrBlank()) {
                logger.info("'$cityName' için şehir kodu '$iataCode' bulundu.")
                return iataCode
            } else {
                throw IllegalArgumentException("'$cityName' şehri için IATA kodu bulunamadı. Yanıt: $data")
            }

        } catch (e: HttpClientErrorException) {
            logger.error("'$cityName' için Amadeus Konum API hatası: ${e.statusCode} - ${e.responseBodyAsString}")
            throw IllegalStateException("'$cityName' için şehir kodu alınırken API hatası oluştu.", e)
        } catch (e: Exception) {
            logger.error("'$cityName' için şehir kodu alınırken beklenmedik hata: ${e.message}", e)
            throw RuntimeException("'$cityName' için şehir kodu alınırken beklenmedik bir hata oluştu.", e)
        }
    }
    private fun formatDuration(isoDuration: String?): String {
        if (isoDuration.isNullOrBlank() || isoDuration == "PT") {
            return "N/A" // Geçersiz veya boş süreler için "N/A" dön
        }

        return try {
            val duration = Duration.parse(isoDuration)
            val hours = duration.toHours()
            val minutes = duration.toMinutes() % 60 // Saatten arta kalan dakikalar

            val parts = mutableListOf<String>()
            if (hours > 0) {
                parts.add("$hours ${if (hours == 1L) "hour" else "hours"}")
            }
            if (minutes > 0) {
                parts.add("$minutes ${if (minutes == 1L) "minute" else "minutes"}")
            }

            // Eğer süre 0 ise "0 minutes" dön, aksi halde birleştir.
            if (parts.isEmpty()) "0 minutes" else parts.joinToString(" ")

        } catch (e: DateTimeParseException) {
            // logger.warn("Süre ('$isoDuration') ayrıştırılamadı. Olduğu gibi döndürülüyor.", e)
            isoDuration // Eğer parse edilemezse, orijinal string'i dön (fallback)
        } catch (e: Exception) {
            // logger.error("Süre ('$isoDuration') işlenirken beklenmedik hata.", e)
            isoDuration // Diğer hatalarda da orijinal string'i dön
        }
    }
}

