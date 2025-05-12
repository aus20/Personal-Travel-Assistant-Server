package com.aus20.service

import com.aus20.domain.Flight
import com.aus20.domain.User
import com.aus20.domain.UserFlightSearch
import com.aus20.repository.FlightRepository
import com.aus20.repository.UserFlightSearchRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import com.aus20.dto.request.FlightSearchRequestDTO
import com.aus20.service.FlightService
import com.aus20.dto.response.RoundTripFlightResponseDTO
import com.aus20.dto.response.FlightResponseDTO
import com.aus20.service.NotificationService
import java.time.format.DateTimeFormatter
import org.springframework.transaction.annotation.Transactional
@Service
class UserFlightSearchService(
    private val userFlightSearchRepository: UserFlightSearchRepository,
    private val flightRepository: FlightRepository,
    private val flightService: FlightService,
    private val notificationService: NotificationService
) {
    private val dateFormatter = DateTimeFormatter.ISO_DATE
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME


    fun saveUserSearch(
        user: User,
        origin: String,
        destination: String,
        departureDate: LocalDate,
        returnDate: LocalDate?,
        maxPrice: Int?,
        flights: List<Flight>
    ): UserFlightSearch {
        // Create UserFlightSearch entity
        val userSearch = UserFlightSearch(
            user = user,
            origin = origin,
            destination = destination,
            departureDate = departureDate,
            returnDate = returnDate,
            maxPrice = maxPrice,
            createdAt = LocalDateTime.now()
        )

        // Save user flight search to get its ID (needed for linking flights)
        val savedSearch = userFlightSearchRepository.save(userSearch)

        // Attach the search to each flight and save them
        val savedFlights = flights.take(10).map {
            it.copy(userFlightSearch = savedSearch)
        }
        flightRepository.saveAll(savedFlights)

        // Return updated UserFlightSearch with attached flights
        return savedSearch.copy(flights = savedFlights.toMutableList())
    }

    fun getUserSearches(user: User): List<UserFlightSearch> {
        return userFlightSearchRepository.findAllByUser(user)
    }
    

    fun saveSearchWithTopFlights(dto: FlightSearchRequestDTO, user: User): UserFlightSearch {
        if (userFlightSearchRepository.existsByUserAndOriginAndDestinationAndDepartureDateAndReturnDate(
                user,
                dto.origin,
                dto.destination,
                LocalDate.parse(dto.departureDate, dateFormatter),
                dto.returnDate?.let { LocalDate.parse(it, dateFormatter) }
            )
        ) { 
            throw IllegalArgumentException("Search already exists")
        }
        val search = userFlightSearchRepository.save(
            UserFlightSearch(
                user = user,
                origin = dto.origin,
                destination = dto.destination,
                departureDate = LocalDate.parse(dto.departureDate, dateFormatter),
                returnDate = dto.returnDate?.let { LocalDate.parse(it, dateFormatter) },
                maxPrice = dto.maxPrice,
                createdAt = LocalDateTime.now()
            )
        )
    
        val flights = flightService.searchFlightsWithFilters(dto)

        val top10Flights = when (flights) {
            is List<*> -> flights.filterIsInstance<FlightResponseDTO>().sortedBy { it.price }.take(10)
            is RoundTripFlightResponseDTO -> {
                val departureTop = flights.departureFlight.sortedBy { it.price }.take(10)
                val returnTop = flights.returnFlight.sortedBy { it.price }.take(10)
                departureTop + returnTop
            }
            else -> emptyList()

        }
        


        val flightEntities = top10Flights.map { flight ->
            Flight(
                origin = flight.origin,
                destination = flight.destination,
                departureTime = LocalDateTime.parse(flight.departureTime, dateTimeFormatter),
                arrivalTime = LocalDateTime.parse(flight.arrivalTime, dateTimeFormatter),
                carrierCode = flight.carrier.split(" ")[0],
                flightNumber = flight.carrier.split(" ")[1],
                duration = flight.duration,
                aircraftCode = flight.aircraftCode,
                cabinClass = flight.cabinClass,
                numberOfStops = flight.numberOfStops,
                price = flight.price,
                currency = flight.currency,
                userFlightSearch = search
            )
        }
    
        flightRepository.saveAll(flightEntities)

        return search
    }

    @Transactional
    fun updateUserSearch(searchId: Long, dto: FlightSearchRequestDTO, user: User): UserFlightSearch {
        val existingSearch = userFlightSearchRepository.findById(searchId)
            .orElseThrow { IllegalArgumentException("Search not found") }

        if (existingSearch.user.id != user.id) {
            throw IllegalAccessException("Unauthorized access")
        }

        // Delete existing flights
        flightRepository.deleteAll(existingSearch.flights)

        // Update search details
        existingSearch.origin = dto.origin
        existingSearch.destination = dto.destination
        existingSearch.departureDate = LocalDate.parse(dto.departureDate, dateFormatter)
        existingSearch.returnDate = dto.returnDate?.let { LocalDate.parse(it, dateFormatter) }
        existingSearch.maxPrice = dto.maxPrice
        existingSearch.createdAt = LocalDateTime.now()

        val updatedSearch = userFlightSearchRepository.save(existingSearch)

        // Perform new flight search
        val flights = flightService.searchFlightsWithFilters(dto)

        val topFlights = when (flights) {
            is List<*> -> flights.filterIsInstance<FlightResponseDTO>().sortedBy { it.price }.take(10)
            is RoundTripFlightResponseDTO -> flights.departureFlight.sortedBy { it.price }.take(10)
            else -> emptyList()
        }

        val flightEntities = topFlights.map { flight ->
            Flight(
                origin = flight.origin,
                destination = flight.destination,
                departureTime = LocalDateTime.parse(flight.departureTime, dateTimeFormatter),
                arrivalTime = LocalDateTime.parse(flight.arrivalTime, dateTimeFormatter),
                carrierCode = flight.carrier.split(" ")[0],
                flightNumber = flight.carrier.split(" ")[1],
                duration = flight.duration,
                aircraftCode = flight.aircraftCode,
                cabinClass = flight.cabinClass,
                numberOfStops = flight.numberOfStops,
                price = flight.price,
                currency = flight.currency,
                userFlightSearch = updatedSearch
            )
        }

        flightRepository.saveAll(flightEntities)

        println("✅ Flight search with ID $searchId successfully updated.")

        return updatedSearch.copy(flights = flightEntities.toMutableList())
    }
    @Transactional
    fun deleteUserSearch(searchId: Long, user: User) {
        val search = userFlightSearchRepository.findById(searchId)
            .orElseThrow { IllegalArgumentException("Search not found") }

        if (search.user.id != user.id) {
            throw IllegalAccessException("Unauthorized access")
        }

        flightRepository.deleteAll(search.flights)
        userFlightSearchRepository.delete(search)

        println("✅ Flight search with ID $searchId successfully deleted.")
    }
    fun executePeriodicSearches() {
        val allSearches = userFlightSearchRepository.findAll()
    
        for (search in allSearches) {
            val requestDto = FlightSearchRequestDTO(
                origin = search.origin,
                destination = search.destination,
                departureDate = search.departureDate.toString(),
                returnDate = search.returnDate?.toString(),
                maxPrice = search.maxPrice
            )
    
            val newResults = flightService.searchFlightsWithFilters(requestDto)
            val newFlights = when (newResults) {
                is List<*> -> newResults.filterIsInstance<FlightResponseDTO>().sortedBy { it.price }.take(10)
                is RoundTripFlightResponseDTO -> (newResults.departureFlight + newResults.returnFlight)
                    .sortedBy { it.price }
                    .take(10)
                else -> emptyList()
            }
    
            if (newFlights.isEmpty()) continue
    
            val existingPrices = search.flights.map { it.price }.sorted()
            val newPrices = newFlights.map { it.price }.sorted()
    
            if (existingPrices != newPrices) {
                println("🔔 Price update for search ${search.id}. Lowest: ${existingPrices.firstOrNull()} → ${newPrices.firstOrNull()}")
    
                val updatedFlights = newFlights.map {
                    Flight(
                        origin = it.origin,
                        destination = it.destination,
                        departureTime = LocalDateTime.parse(it.departureTime, dateTimeFormatter),
                        arrivalTime = LocalDateTime.parse(it.arrivalTime, dateTimeFormatter),
                        carrierCode = it.carrier.split(" ")[0],
                        flightNumber = it.carrier.split(" ")[1],
                        duration = it.duration,
                        aircraftCode = it.aircraftCode,
                        cabinClass = it.cabinClass,
                        numberOfStops = it.numberOfStops,
                        price = it.price,
                        currency = it.currency,
                        userFlightSearch = search
                    )
                }
    
                search.flights.clear()
                search.flights.addAll(updatedFlights)
                flightRepository.saveAll(updatedFlights)
    
                // Send notification if user has FCM token
                val fcmToken = search.user.fcmToken
                if (!fcmToken.isNullOrBlank()) {
                    val oldPrice = existingPrices.firstOrNull()
                    val newPrice = newPrices.firstOrNull()
                    val diff = if (oldPrice != null && newPrice != null) oldPrice - newPrice else 0

                    notificationService.sendNotification(
                        token = fcmToken,
                        title = "Flight Price Update ",
                        body = "New lowest price: $newPrice (↓ $diff from $oldPrice)"
                    )
                }
            }
        }
    }
    
}
