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

@Service
@Profile("!dev")
class FlightService(
    private val authService: AmadeusAuthService,
    private val restTemplate: RestTemplate
) : FlightDataProvider {
    // searchflights may take more than 2 variables such as departureDate, adults, children, infants, etc.
    // this will change in the future
    fun searchFlights(origin: String, destination: String): List<Flight> {
        val token = authService.getAccessToken()
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            setBearerAuth(token)
        }

        val url = "https://test.api.amadeus.com/v2/shopping/flight-offers" +
                "?originLocationCode=$origin&destinationLocationCode=$destination" +
                "&departureDate=2025-05-05&adults=1"

        val response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            HttpEntity<String>(headers),
            Map::class.java
        )

        


        val data = response.body?.get("data") as? List<*> ?: return emptyList()

        return data.mapNotNull { offerRaw ->
            val offer = offerRaw as? Map<String, Any>
            if (offer == null) {
                println("Offer is not a map: $offerRaw")
                return@mapNotNull null
            }

            try {
                val itineraries = (offer["itineraries"] as? List<*>)?.filterIsInstance<Map<String, Any>>()
                if (itineraries.isNullOrEmpty()) {
                    println("Missing or empty itineraries")
                    return@mapNotNull null
                }
    
                val firstItinerary = itineraries.first()
                val segments = (firstItinerary["segments"] as? List<*>)?.filterIsInstance<Map<String, Any>>()
                if (segments.isNullOrEmpty()) {
                    println("Missing or empty segments")
                    return@mapNotNull null
                }
    
                val firstSegment = segments.first()
    
                val departure = (firstSegment["departure"] as? Map<String, Any>) ?: return@mapNotNull null
                val arrival = (firstSegment["arrival"] as? Map<String, Any>) ?: return@mapNotNull null
    
                val departureTimeRaw = departure["at"] as? String ?: return@mapNotNull null
                val arrivalTimeRaw = arrival["at"] as? String ?: return@mapNotNull null
    
                val formatter = DateTimeFormatter.ISO_DATE_TIME
                val departureTime = try {
                    LocalDateTime.parse(departureTimeRaw, formatter)
                } catch (e: Exception) {
                    println("Failed to parse departure time: $departureTimeRaw")
                    return@mapNotNull null
                }
    
                val arrivalTime = try {
                    LocalDateTime.parse(arrivalTimeRaw, formatter)
                } catch (e: Exception) {
                    println("Failed to parse arrival time: $arrivalTimeRaw")
                    return@mapNotNull null
                }
    
                val carrierCode = firstSegment["carrierCode"] as? String ?: "??"
                val flightNumber = firstSegment["number"] as? String ?: "??"
                val aircraftCode = (firstSegment["aircraft"] as? Map<*, *>)?.get("code") as? String ?: "N/A"
                
                val duration = firstItinerary["duration"] as? String ?: "N/A"
                val numberOfStops = segments.size - 1

                val priceBlock = offer["price"] as? Map<*, *> ?: return@mapNotNull null
                val price = (priceBlock["total"] as? String)?.toDoubleOrNull() ?: 0.0
                val currency = priceBlock["currency"] as? String ?: "USD"
                
                val travelerPricings = offer["travelerPricings"] as? List<*> ?: return@mapNotNull null
                val firstTraveler = travelerPricings.firstOrNull() as? Map<*, *> ?: return@mapNotNull null
                val fareDetails = firstTraveler["fareDetailsBySegment"] as? List<*> ?: return@mapNotNull null
                val firstFare = fareDetails.firstOrNull() as? Map<*, *> ?: return@mapNotNull null
                val cabinClass = firstFare["cabin"] as? String ?: "Unknown"

                Flight(
                    origin = origin,
                    destination = destination,
                    departureTime = departureTime,
                    arrivalTime = arrivalTime,
                    carrierCode = carrierCode,
                    flightNumber = flightNumber,
                    duration = duration,
                    aircraftCode = aircraftCode,
                    cabinClass = cabinClass,
                    numberOfStops = numberOfStops,
                    price = price,
                    currency = currency
                )
            } catch (e: Exception) {
                println("Mapping failed for offer: ${offer["id"] ?: "unknown"} -> ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }
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

    override fun searchFlightsWithFilters(request: FlightSearchRequestDTO): Any {
        val token = authService.getAccessToken()
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            setBearerAuth(token)
        }

        val departureFlights = fetchFlights(
            origin = request.origin,
            destination = request.destination,
            date = request.departureDate,
            adults = request.adults,
            preferredAirlines = request.preferredAirlines,
            maxPrice = request.maxPrice,
            headers = headers
        )

        if (request.isRoundTrip && request.returnDate != null) {
            val returnFlights = fetchFlights(
                origin = request.destination,
                destination = request.origin,
                date = request.returnDate,
                adults = request.adults,
                preferredAirlines = request.preferredAirlines,
                maxPrice = request.maxPrice,
                headers = headers
            )
            return RoundTripFlightResponseDTO(
                departureFlight = departureFlights,
                returnFlight = returnFlights
            )
        }

        return departureFlights
    }

    private fun fetchFlights(
        origin: String,
        destination: String,
        date: String,
        adults: Int,
        preferredAirlines: List<String>?,
        maxPrice: Int?,
        headers: HttpHeaders
    ): List<FlightResponseDTO> {
        val baseUrl = "https://test.api.amadeus.com/v2/shopping/flight-offers"
        val urlBuilder = StringBuilder("$baseUrl?originLocationCode=$origin&destinationLocationCode=$destination&departureDate=$date&adults=$adults")

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
            println("🔁 Too many requests: ${e.statusCode} - ${e.responseBodyAsString}")
            return emptyList() // or throw a custom exception, or add retry logic
        } catch (e: HttpClientErrorException) {
            println("❌ API error: ${e.statusCode} - ${e.responseBodyAsString}")
            return emptyList()
        }

        val data = response.body?.get("data") as? List<*> ?: return emptyList()

        return data.mapNotNull { offerRaw ->
            val offer = offerRaw as? Map<String, Any> ?: return@mapNotNull null
            try {
                val itineraries = (offer["itineraries"] as? List<*>)?.filterIsInstance<Map<String, Any>>()
                if (itineraries.isNullOrEmpty()) return@mapNotNull null

                val firstItinerary = itineraries.first()
                val segments = (firstItinerary["segments"] as? List<*>)?.filterIsInstance<Map<String, Any>>()
                if (segments.isNullOrEmpty()) return@mapNotNull null

                val firstSegment = segments.first()
                val departure = firstSegment["departure"] as? Map<String, Any> ?: return@mapNotNull null
                val arrival = firstSegment["arrival"] as? Map<String, Any> ?: return@mapNotNull null

                val departureTimeRaw = departure["at"] as? String ?: return@mapNotNull null
                val arrivalTimeRaw = arrival["at"] as? String ?: return@mapNotNull null

                val formatter = DateTimeFormatter.ISO_DATE_TIME
                val departureTime = LocalDateTime.parse(departureTimeRaw, formatter)
                val arrivalTime = LocalDateTime.parse(arrivalTimeRaw, formatter)

                val carrierCode = firstSegment["carrierCode"] as? String ?: return@mapNotNull null
                val flightNumber = firstSegment["number"] as? String ?: return@mapNotNull null
                val aircraftCode = (firstSegment["aircraft"] as? Map<*, *>)?.get("code") as? String ?: "N/A"
                val duration = firstItinerary["duration"] as? String ?: "N/A"
                val numberOfStops = segments.size - 1

                val priceBlock = offer["price"] as? Map<*, *> ?: return@mapNotNull null
                val price = (priceBlock["total"] as? String)?.toDoubleOrNull() ?: return@mapNotNull null
                val currency = priceBlock["currency"] as? String ?: "USD"

                val travelerPricings = offer["travelerPricings"] as? List<*> ?: return@mapNotNull null
                val firstTraveler = travelerPricings.firstOrNull() as? Map<*, *> ?: return@mapNotNull null
                val fareDetails = firstTraveler["fareDetailsBySegment"] as? List<*> ?: return@mapNotNull null
                val firstFare = fareDetails.firstOrNull() as? Map<*, *> ?: return@mapNotNull null
                val cabinClass = firstFare["cabin"] as? String ?: "Unknown"

                FlightResponseDTO(
                    origin = origin,
                    destination = destination,
                    departureTime = departureTime.toString(),
                    arrivalTime = arrivalTime.toString(),
                    carrier = "$carrierCode $flightNumber",
                    duration = duration,
                    aircraftCode = aircraftCode,
                    cabinClass = cabinClass,
                    numberOfStops = numberOfStops,
                    price = price,
                    currency = currency
                )
            } catch (e: Exception) {
                println("Mapping failed for offer: ${offer["id"] ?: "unknown"} -> ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }
}

