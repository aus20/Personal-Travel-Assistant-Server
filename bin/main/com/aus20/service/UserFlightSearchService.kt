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
import com.aus20.service.FlightDataProvider
import com.aus20.dto.response.RoundTripFlightResponseDTO
import com.aus20.dto.response.FlightResponseDTO
import com.aus20.service.NotificationService
import java.time.format.DateTimeFormatter
import com.aus20.dto.response.SavedUserSearchResponseDTO
import org.springframework.transaction.annotation.Transactional
import com.aus20.dto.response.UserSearchDetailDTO
import org.slf4j.LoggerFactory
import java.time.format.DateTimeParseException
import com.aus20.dto.enums.FlightLegType

@Service
class UserFlightSearchService(
    private val userFlightSearchRepository: UserFlightSearchRepository,
    private val flightRepository: FlightRepository,
    private val flightDataProvider: FlightDataProvider,
    private val notificationService: NotificationService
) {
    private val logger = LoggerFactory.getLogger(UserFlightSearchService::class.java)
    private val dateFormatter = DateTimeFormatter.ISO_DATE
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    // bu fonksiyon zaten saveSearchWithTopFlights ile aynı işi yapıyor. flight'larını save etmek yerine.
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

    fun getUserSearches(user: User): List<UserSearchDetailDTO> {
        // Kullanıcıya ait tüm UserFlightSearch kayıtlarını çek.
        // İlişkili 'flights' koleksiyonunun da yüklenmesini sağlamak önemli.
        // userFlightSearchRepository'de özel bir sorgu (JOIN FETCH ile) tanımlayabilirsin
        // veya FetchType.EAGER kullanabilirsin (dikkatli ol).
        // Şimdilik, transaction içinde olduğumuz için lazy loading çalışacaktır.
        val userSearches = userFlightSearchRepository.findAllByUser(user)
        return userSearches.map { searchEntity ->
            // Her bir UserFlightSearch entity'sini UserSearchDetailDTO'ya map et

            // İlişkili Flight entity'lerini FlightResponseDTO'ya map et
            val flightDTOs = searchEntity.flights.map { flightEntity ->
                val legType: FlightLegType = if (searchEntity.returnDate != null) { 
                    if (flightEntity.origin == searchEntity.origin && flightEntity.destination == searchEntity.destination) {
                        FlightLegType.DEPARTURE
                    } else if (flightEntity.origin == searchEntity.destination && flightEntity.destination == searchEntity.origin) {
                        FlightLegType.RETURN
                    } else {
                        // Bu durum normalde tutarlı veride olmamalı.
                        // Bir gidiş-dönüş aramasındaki uçuş ya gidiş ya da dönüş olmalı.
                        // Hata yönetimi veya loglama için buraya bir not düşülebilir.
                        logger.warn(
                            "Belirlenemeyen uçuş ayağı (gidiş-dönüş aramasında): Search ID {}, Flight ID {}, Flight Origin: {}, Flight Dest: {}, Search Origin: {}, Search Dest: {}",
                            searchEntity.id, flightEntity.id, flightEntity.origin, flightEntity.destination, searchEntity.origin, searchEntity.destination
                        )
                        // Varsayılan bir değer atamak yerine hata fırlatmak veya özel bir UNKNOWN tipi de düşünülebilir.
                        // Şimdilik, mantıksal olarak bir ayağa ait olmalı, bu yüzden birini seçmek yerine loglayıp
                        // verinin neden böyle olduğunu araştırmaya yönlendirmek daha iyi olabilir.
                        // Veya FlightLegType'a UNKNOWN gibi bir değer ekleyip onu kullanabilirsiniz.
                        // Geçici olarak, eğer bu durum oluşursa, onu DEPARTURE gibi bir şeye atayalım ama bu ideal değil.
                        FlightLegType.DEPARTURE // Ya da daha uygun bir varsayılan/hata durumu
                    }
                } else { // Tek yönlü arama ise
                    FlightLegType.ONE_WAY
                }
                FlightResponseDTO(
                    origin = flightEntity.origin,
                    destination = flightEntity.destination,
                    departureTime = flightEntity.departureTime.format(dateTimeFormatter),
                    arrivalTime = flightEntity.arrivalTime.format(dateTimeFormatter),
                    carrier = "${flightEntity.carrierCode} ${flightEntity.flightNumber}",
                    duration = flightEntity.duration,
                    aircraftCode = flightEntity.aircraftCode,
                    cabinClass = flightEntity.cabinClass,
                    numberOfStops = flightEntity.numberOfStops,
                    price = flightEntity.price,
                    currency = flightEntity.currency,
                    leg = legType
                )
            }.sortedBy { it.price } // Uçuşları fiyata göre sıralayabiliriz (isteğe bağlı)

            UserSearchDetailDTO(
                id = searchEntity.id,
                origin = searchEntity.origin,
                destination = searchEntity.destination,
                departureDate = searchEntity.departureDate.format(dateFormatter),
                returnDate = searchEntity.returnDate?.format(dateFormatter),
                isRoundTrip = searchEntity.returnDate != null, // returnDate varsa gidiş-dönüştür
                maxPrice = searchEntity.maxPrice,
                createdAt = searchEntity.createdAt.format(dateTimeFormatter),
                adults = searchEntity.adults,// <<<--- searchEntity'den adults'ı al
                preferredAirlines = searchEntity.preferredAirlines?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }, // <<<--- String'i List<String>'e çevir // Oluşturulma zamanı
                flights = flightDTOs // Map edilmiş FlightResponseDTO listesi
            )
        }
    }
    

    fun saveSearchWithTopFlights(dto: FlightSearchRequestDTO, user: User): SavedUserSearchResponseDTO {
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
        val searchEntity =
            UserFlightSearch(
                user = user,
                origin = dto.origin,
                destination = dto.destination,
                departureDate = LocalDate.parse(dto.departureDate, dateFormatter),
                returnDate = dto.returnDate?.let { LocalDate.parse(it, dateFormatter) },
                maxPrice = dto.maxPrice,
                adults = dto.adults,
                preferredAirlines = dto.preferredAirlines?.joinToString(","),
                isRoundTrip = dto.isRoundTrip,
                createdAt = LocalDateTime.now()
            )
        val savedSearchEntity = userFlightSearchRepository.save(searchEntity)
    
        val flightsResult = flightDataProvider.searchFlightsWithFilters(dto)

        val top10Flights = when (flightsResult) {
            is List<*> -> flightsResult.filterIsInstance<FlightResponseDTO>().sortedBy { it.price }.take(10)
            is RoundTripFlightResponseDTO -> {
                val departureTop = flightsResult.departureFlight.sortedBy { it.price }.take(10)
                val returnTop = flightsResult.returnFlight.sortedBy { it.price }.take(10)
                departureTop + returnTop
            }
            else -> {
                // TODO: handle this with a better error message
                println("⚠️ Unexpected result type from flightDataProvider during saveSearchWithTopFlights: ${flightsResult::class.simpleName} for search id ${savedSearchEntity.id}")
                emptyList()
            }

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
                userFlightSearch = savedSearchEntity
            )
        }
    
        if (flightEntities.isNotEmpty()){ // flightEntities boş değilse kaydet
            flightRepository.saveAll(flightEntities)
       }


       // Şimdi SavedUserSearchResponseDTO'yu oluşturup döndür
       return SavedUserSearchResponseDTO( // <<< --- DEĞİŞİKLİK BURADA: Yeni return ifadesi ---
           searchId = savedSearchEntity.id,
           message = "Search saved successfully",
           origin = savedSearchEntity.origin,
           destination = savedSearchEntity.destination,
           departureDate = savedSearchEntity.departureDate.format(dateFormatter),
           returnDate = savedSearchEntity.returnDate?.format(dateFormatter),
           maxPrice = savedSearchEntity.maxPrice,
           adults = savedSearchEntity.adults,
           preferredAirlines = savedSearchEntity.preferredAirlines?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() },
           savedFlights = top10Flights // Bu zaten List<FlightResponseDTO> idi
       )
   }

    @Transactional
    fun updateUserSearch(searchId: Long, dto: FlightSearchRequestDTO, user: User): UserSearchDetailDTO {
        val existingSearch = userFlightSearchRepository.findById(searchId)
            .orElseThrow { IllegalArgumentException("Search with ID $searchId not found") }

        if (existingSearch.user.id != user.id) {
            throw IllegalAccessException("Unauthorized access")
        }

        // Delete existing flights
        flightRepository.deleteAll(existingSearch.flights)

        // Clear existing flights WHY? Because we are going to add new flights to it.
        existingSearch.flights.clear()
        // Update search details
        existingSearch.origin = dto.origin
        existingSearch.destination = dto.destination
        existingSearch.departureDate = LocalDate.parse(dto.departureDate, dateFormatter)
        existingSearch.returnDate = dto.returnDate?.let { LocalDate.parse(it, dateFormatter) }
        existingSearch.maxPrice = dto.maxPrice
        existingSearch.createdAt = LocalDateTime.now()

        val updatedSearchEntity = userFlightSearchRepository.save(existingSearch) // Güncellenmiş arama entity'si

        // Perform new flight search
        val flightsResult = flightDataProvider.searchFlightsWithFilters(dto)

        val topFlights = when (flightsResult) {
            is List<*> -> flightsResult.filterIsInstance<FlightResponseDTO>().sortedBy { it.price }.take(10)
            is RoundTripFlightResponseDTO -> {
                val departureTop = flightsResult.departureFlight.sortedBy { it.price }.take(10)
                val returnTop = flightsResult.returnFlight.sortedBy { it.price }.take(10)
                departureTop + returnTop
            }
            else -> {
                logger.error("Unexpected result type from flightDataProvider during updateUserSearch: ${flightsResult::class.simpleName} for search id ${updatedSearchEntity.id}")
                emptyList()
            }
        }

        val newflightEntities = topFlights.map { flight ->
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
                userFlightSearch = updatedSearchEntity
            )
        }

        flightRepository.saveAll(newflightEntities)

        println("Flight search with ID $searchId successfully updated.")

        // Yanıt olarak UserSearchDetailDTO döndür
        return UserSearchDetailDTO(
            id = updatedSearchEntity.id,
            origin = updatedSearchEntity.origin,
            destination = updatedSearchEntity.destination,
            departureDate = updatedSearchEntity.departureDate.format(dateFormatter),
            returnDate = updatedSearchEntity.returnDate?.format(dateFormatter),
            isRoundTrip = updatedSearchEntity.returnDate != null,
            maxPrice = updatedSearchEntity.maxPrice,
            createdAt = updatedSearchEntity.createdAt.format(dateTimeFormatter),
            adults = updatedSearchEntity.adults,
            preferredAirlines = updatedSearchEntity.preferredAirlines?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() },
            flights = topFlights // Yeni bulunan ve DTO formatında olan uçuşlar
        )
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

        println("Flight search with ID $searchId successfully deleted.")
    }
    /*
    @Transactional
    fun executePeriodicSearches() {
        val allSearches = userFlightSearchRepository.findAllWithUserAndFlights()
        logger.info("Executing periodic searches for ${allSearches.size} searches")
    
        for (search in allSearches) {
            logger.debug("Processing search ID: ${search.id} for user ID: ${search.user.id}")
            val requestDto = FlightSearchRequestDTO(
                origin = search.origin,
                destination = search.destination,
                departureDate = search.departureDate.format(dateFormatter),
                returnDate = search.returnDate?.format(dateFormatter),
                isRoundTrip = search.returnDate != null,
                maxPrice = search.maxPrice,
                adults = search.adults,
                preferredAirlines = search.preferredAirlines?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
                
            )
    
            val newResults = flightDataProvider.searchFlightsWithFilters(requestDto)
            val newFlights = when (newResults) {
                is List<*> -> newResults.filterIsInstance<FlightResponseDTO>().sortedBy { it.price }.take(10)
                is RoundTripFlightResponseDTO -> {
                    val departureTop = newResults.departureFlight.sortedBy { it.price }.take(10)
                    val returnTop = newResults.returnFlight.sortedBy { it.price }.take(10)
                    departureTop + returnTop
                }
                else -> {
                    logger.warn("Unexpected result type from flightDataProvider during executePeriodicSearches: ${newResults::class.simpleName} for search id ${search.id}")
                    emptyList()
                }
            }
    
            if (newFlights.isEmpty()) {
                logger.info("No flights found for search ID: ${search.id}")
                continue
            }

            val oldLowestPrice = search.flights.minOfOrNull { it.price }
            val newLowestPrice = newFlights.minOfOrNull { it.price }
            val newLowestCurrency = newFlights.firstOrNull()?.currency ?: search.flights.firstOrNull()?.currency ?: "N/A"

            var significantChange = false
            var notificationBody = ""

            if (newLowestPrice != null) {
                // Detaylı loglama ekleyin:
                logger.info("Search ID {}: Değerlendirme: oldLowestPrice: {}, newLowestPrice: {}, search.maxPrice: {}",
                    search.id, oldLowestPrice, newLowestPrice, search.maxPrice)

                if (oldLowestPrice == null || newLowestPrice < oldLowestPrice) {
                    significantChange = true
                    val oldPriceDisplay = oldLowestPrice?.toString() ?: "previously no flights"
                    notificationBody = "Price drop for ${search.origin}-${search.destination}! New lowest: $newLowestCurrency $newLowestPrice (was $oldPriceDisplay)."
                    logger.info("Price drop for search ${search.id}. Old lowest: $oldPriceDisplay → New lowest: $newLowestPrice $newLowestCurrency")
                } else { 
                    val localCurrentMaxPrice = search.maxPrice
                    logger.info("Search ID {}: Fiyat düşüşü yok. Max Fiyat Kontrolü -> localCurrentMaxPrice: {}, newLowestPrice <= localCurrentMaxPrice: {}, oldLowestPrice ({}) > localCurrentMaxPrice ({}): {}",
                    search.id, localCurrentMaxPrice, newLowestPrice, oldLowestPrice, localCurrentMaxPrice)

                    if (localCurrentMaxPrice != null && newLowestPrice <= localCurrentMaxPrice && oldLowestPrice > localCurrentMaxPrice) {
                    significantChange = true
                    notificationBody = "Price alert for ${search.origin}-${search.destination}! Flights now available under your max price of ${search.maxPrice}. New lowest: $newLowestCurrency $newLowestPrice."
                    logger.info("Price alert (under maxPrice) for search ${search.id}. New lowest: $newLowestPrice $newLowestCurrency")
                    }
                }
            }

            if (significantChange) {
                // Adım 5a: Eski ilişkili uçuşları SİL (JPA Cascade ve Orphan Removal ile)
                search.flights.clear() // Bu, orphanRemoval=true ile DB'den silinmelerini tetikler.

                // Adım 5b: Yeni bulunan uçuşları Flight entity'sine çevir ve search'ün koleksiyonuna EKLE
                val updatedFlightEntities = newFlights.map { flightDto ->
                    Flight(
                        origin = flightDto.origin,
                        destination = flightDto.destination,
                        departureTime = LocalDateTime.parse(flightDto.departureTime, dateTimeFormatter),
                        arrivalTime = LocalDateTime.parse(flightDto.arrivalTime, dateTimeFormatter),
                        carrierCode = flightDto.carrier.split(" ").firstOrNull() ?: flightDto.carrier,
                        flightNumber = flightDto.carrier.split(" ").getOrNull(1) ?: "",
                        duration = flightDto.duration,
                        aircraftCode = flightDto.aircraftCode,
                        cabinClass = flightDto.cabinClass,
                        numberOfStops = flightDto.numberOfStops,
                        price = flightDto.price,
                        currency = flightDto.currency,
                        userFlightSearch = search // İlişkiyi kur
                    )
                }
                search.flights.addAll(updatedFlightEntities)

                // Adım 5c: UserFlightSearch entity'sini KAYDET.
                // CascadeType.ALL sayesinde, 'search.flights' koleksiyonundaki değişiklikler
                // (eskilerin silinmesi, yenilerin eklenmesi) otomatik olarak veritabanına yansıtılır.
                userFlightSearchRepository.save(search) // Sadece ana entity'yi save etmek yeterli
    
            
    
                // Send notification if user has FCM token
                val fcmToken = search.user.fcmToken
                if (!fcmToken.isNullOrBlank()) {
                    notificationService.sendNotification(
                        token = fcmToken,
                        title = "Flight Price Update",
                        body = notificationBody
                    )
                    logger.info("Notification sent to user ${search.user.id} for search ${search.id}")
                }
            }   else {
                logger.info("No significant change for search ${search.id}")
                }
        }
    }
    */
    @Transactional
    fun executePeriodicSearches() {
        val allSearches = userFlightSearchRepository.findAllWithUserAndFlights()
        logger.info("Executing periodic searches for ${allSearches.size} searches")

        for (search in allSearches) {
            logger.debug("Processing search ID: ${search.id} for user ID: ${search.user.id}")
            val requestDto = FlightSearchRequestDTO(
                origin = search.origin,
                destination = search.destination,
                departureDate = search.departureDate.format(dateFormatter),
                returnDate = search.returnDate?.format(dateFormatter),
                isRoundTrip = search.returnDate != null,
                maxPrice = search.maxPrice, // Sağlayıcıya gönderilecek, sağlayıcı filtreleyecek
                adults = search.adults,
                preferredAirlines = search.preferredAirlines?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            )

            val newResultsFromProvider = flightDataProvider.searchFlightsWithFilters(requestDto)

            // Önce mevcut (eski) uçuşları bildirim karşılaştırması için yedekle
            val savedFlightsCopy = search.flights.toList() // ÖNEMLİ: Kopyasını al

            // Veritabanını her zaman API'den gelen yeni verilerle güncelle
            search.flights.clear() // Mevcut bağlı uçuşları temizle (orphanRemoval ile silinecekler)
            var dbUpdated = false

            val finalNotificationMessages = mutableListOf<String>()

            when (newResultsFromProvider) {
                is RoundTripFlightResponseDTO -> {
                    logger.info("Search ID ${search.id} (Round Trip): Updating DB with new flights.")
                    val newApiDepartureFlights = newResultsFromProvider.departureFlight.sortedBy { it.price }.take(10) // Veya N
                    val newApiReturnFlights = newResultsFromProvider.returnFlight.sortedBy { it.price }.take(10)    // Veya N

                    newApiDepartureFlights.forEach { flightDto ->
                        try {
                            search.flights.add(mapFlightDtoToEntity(flightDto, search))
                            dbUpdated = true
                        } catch (e: DateTimeParseException) { /* ... log ... */ }
                    }
                    newApiReturnFlights.forEach { flightDto ->
                        try {
                            search.flights.add(mapFlightDtoToEntity(flightDto, search))
                            dbUpdated = true
                        } catch (e: DateTimeParseException) { /* ... log ... */ }
                    }

                    if (dbUpdated) { // Sadece gerçekten yeni entity eklendiyse save et
                        userFlightSearchRepository.save(search)
                        logger.info("Search ID ${search.id} (Round Trip): DB updated with ${search.flights.size} flights.")
                    } else {
                        logger.info("Search ID ${search.id} (Round Trip): No new valid flights from API to update DB.")
                    }


                    // Bildirim için eski ve yeni verileri karşılaştır
                    val (eskiGidis, eskiDonus) = partitionSavedFlights(savedFlightsCopy, search.origin, search.destination)

                    val gidisNotifInfo = determineNotificationForLeg(
                        eskiGidis, newApiDepartureFlights, "gidiş", search.origin, search.destination
                    )
                    gidisNotifInfo.messagePart?.let { finalNotificationMessages.add(it) }

                    val donusNotifInfo = determineNotificationForLeg(
                        eskiDonus, newApiReturnFlights, "dönüş", search.destination, search.origin
                    )
                    donusNotifInfo.messagePart?.let { finalNotificationMessages.add(it) }
                }
                is List<*> -> { // Tek Yönlü Arama
                    val newApiOneWayFlights = newResultsFromProvider.filterIsInstance<FlightResponseDTO>().sortedBy { it.price }.take(10) // Veya N
                    logger.info("Search ID ${search.id} (One Way): Updating DB with new flights.")

                    newApiOneWayFlights.forEach { flightDto ->
                        try {
                            search.flights.add(mapFlightDtoToEntity(flightDto, search))
                            dbUpdated = true
                        } catch (e: DateTimeParseException) { /* ... log ... */ }
                    }

                    if (dbUpdated) {
                        userFlightSearchRepository.save(search)
                        logger.info("Search ID ${search.id} (One Way): DB updated with ${search.flights.size} flights.")
                    } else {
                        logger.info("Search ID ${search.id} (One Way): No new valid flights from API to update DB.")
                    }

                    // Bildirim için eski ve yeni verileri karşılaştır
                    val tekYonNotifInfo = determineNotificationForLeg(
                        savedFlightsCopy, newApiOneWayFlights, "uçuş", search.origin, search.destination
                    )
                    tekYonNotifInfo.messagePart?.let { finalNotificationMessages.add(it) }
                }
                else -> {
                    logger.warn("Unexpected result type from flightDataProvider: ${newResultsFromProvider::class.simpleName} for search ID ${search.id}")
                }
            }

            // Oluşturulan bildirim mesajlarını gönder
            if (finalNotificationMessages.isNotEmpty()) {
                val fullNotificationBody = finalNotificationMessages.joinToString(" ")
                val fcmToken = search.user.fcmToken
                if (!fcmToken.isNullOrBlank()) {
                    try {
                        notificationService.sendNotification(
                            token = fcmToken,
                            title = "${search.origin}-${search.destination} Uçuş Fiyat Değişikliği", // Daha genel bir başlık
                            body = fullNotificationBody
                        )
                        logger.info("Bildirim gönderildi: Kullanıcı ${search.user.id}, Arama ${search.id}. Mesaj: $fullNotificationBody")
                    } catch (e: Exception) {
                        logger.error("Bildirim gönderilemedi (Kullanıcı ID: ${search.user.id}, Arama ID: ${search.id}): ${e.message}")
                    }
                } else {
                    logger.info("Bildirim için değişiklik bulundu (Arama ${search.id}) ama kullanıcının FCM token'ı yok. Mesaj: $fullNotificationBody")
                }
            } else if (dbUpdated) { // DB güncellendi ama bildirimlik bir durum yoksa
                logger.info("Veritabanı güncellendi (Arama ${search.id}), ancak bildirim için önemli bir fiyat değişikliği tespit edilmedi.")
            } else { // Ne DB güncellendi ne de bildirimlik durum var (API'den hiç geçerli uçuş gelmediyse)
                logger.info("No database update and no significant change for notification for search ID ${search.id}.")
            }
        } // for döngüsü sonu
    }

    // mapFlightDtoToEntity yardımcı fonksiyonu (UserFlightSearchService içinde private):
    private fun mapFlightDtoToEntity(flightDto: FlightResponseDTO, search: UserFlightSearch): Flight {
        // Bu fonksiyon, DateTimeParseException hatasını fırlatabilir, çağıran yer try-catch yapmalı.
        // Ya da bu fonksiyonun içinde try-catch yapılıp null dönülebilir ve çağıran yer filterNotNull kullanabilir.
        // Yukarıdaki ana kodda çağıran yerde try-catch yaptım.
        return Flight(
            origin = flightDto.origin,
            destination = flightDto.destination,
            departureTime = LocalDateTime.parse(flightDto.departureTime, dateTimeFormatter),
            arrivalTime = LocalDateTime.parse(flightDto.arrivalTime, dateTimeFormatter),
            carrierCode = flightDto.carrier.split(" ").firstOrNull() ?: flightDto.carrier,
            flightNumber = flightDto.carrier.split(" ").getOrNull(1) ?: "",
            duration = flightDto.duration,
            aircraftCode = flightDto.aircraftCode,
            cabinClass = flightDto.cabinClass,
            numberOfStops = flightDto.numberOfStops,
            price = flightDto.price,
            currency = flightDto.currency,
            userFlightSearch = search
        )
    }
    // UserFlightSearchService.kt içinde private bir fonksiyon:
    private fun partitionSavedFlights(
        savedFlights: List<Flight>, // .toList() ile kopyası verilecek
        searchOrigin: String,
        searchDestination: String
    ): Pair<List<Flight>, List<Flight>> {
        val departureFlights = mutableListOf<Flight>()
        val returnFlights = mutableListOf<Flight>()

        for (flight in savedFlights) {
            if (flight.origin == searchOrigin && flight.destination == searchDestination) {
                departureFlights.add(flight)
            } else if (flight.origin == searchDestination && flight.destination == searchOrigin) {
                returnFlights.add(flight)
            }
            // else: Bu uçuş ne gidiş ne de dönüş kriterine uymuyor (belki eski bir mantıktan kalma?)
            // Şimdilik görmezden geliyoruz ya da loglayabilirsiniz.
        }
        return Pair(departureFlights, returnFlights)
    }
    // UserFlightSearchService.kt içinde private bir fonksiyon:
    private fun determineNotificationForLeg(
        oldDbLegFlights: List<Flight>,
        newApiLegFlights: List<FlightResponseDTO>,
        // searchMaxPrice: Int?, // Bu artık bildirim için direkt kullanılmıyor, çünkü maxPrice kontrolü zaten veri alımında var.
                            // Ancak, eğer "maxPrice altına yeni düştü" gibi bir bildirim istenirse tekrar eklenebilir.
                            // Şimdilik siliyoruz, çünkü sadece düşüş/artışa odaklandık.
        legName: String, // Örn: "gidiş", "dönüş"
        legOrigin: String,
        legDestination: String
    ): LegNotificationInfo {
        val oldLegLowestPrice = oldDbLegFlights.minOfOrNull { it.price }
        val newLegLowestPrice = newApiLegFlights.minOfOrNull { it.price }

        // Loglama için (bir önceki cevaptaki gibi detaylı logları buraya ekleyebilirsiniz)
        logger.debug("Leg '$legName' ($legOrigin-$legDestination) Değerlendirme: Eski En Düşük Fiyat: $oldLegLowestPrice, Yeni En Düşük Fiyat: $newLegLowestPrice")


        if (newLegLowestPrice == null) { // API'den bu ayak için yeni uçuş gelmedi veya gelenlerin fiyatı yok.
            if (oldLegLowestPrice != null) {
                // Eskiden uçuş vardı ama şimdi yok. Bu durumu bildirmek isteyebilir misiniz?
                // Şimdilik mevcut isteğinizde bu yok, o yüzden NONE dönüyoruz.
                logger.info("Leg '$legName' ($legOrigin-$legDestination): Yeni uçuş bulunamadı, eskiden vardı.")
                return LegNotificationInfo(NotificationReason.NONE) // Veya özel bir NotificationReason.FLIGHTS_DISAPPEARED
            }
            return LegNotificationInfo(NotificationReason.NONE) // Ne eski ne yeni uçuş var, değişiklik yok.
        }

        val newLegLowestCurrency = newApiLegFlights.firstOrNull()?.currency
            ?: oldDbLegFlights.firstOrNull()?.currency
            ?: "N/A"

        // 1. Fiyat Düşüşü Kontrolü
        if (oldLegLowestPrice == null || newLegLowestPrice < oldLegLowestPrice) {
            val oldPriceDisplay = oldLegLowestPrice?.toString() ?: "daha önce uçuş yoktu"
            val message =
                "$legOrigin-$legDestination $legName uçuşlarında fiyat düştü! Yeni en düşük: $newLegLowestCurrency $newLegLowestPrice (önceki: $oldPriceDisplay)."
            logger.info("$message")
            return LegNotificationInfo(NotificationReason.PRICE_DROP, message)
        }

        // 2. Fiyat Artışı Kontrolü (Sadece fiyat düşüşü yoksa ve eski fiyat varsa mantıklı)
        if (newLegLowestPrice > oldLegLowestPrice) {
            val oldPriceDisplay = oldLegLowestPrice.toString()
            val message =
                "$legOrigin-$legDestination $legName uçuşlarında en düşük fiyat arttı. Yeni en düşük: $newLegLowestCurrency $newLegLowestPrice (önceki: $oldPriceDisplay)." // oldPriceDisplay yukarıda tanımlı
            logger.info("$message")
            return LegNotificationInfo(NotificationReason.PRICE_INCREASE, message)
        }

        // Diğer durumlar için (fiyat aynı kaldı veya sadece uçuşlar değişti ama en düşük fiyat değişmedi)
        return LegNotificationInfo(NotificationReason.NONE)
    }
    // UserFlightSearchService.kt içinde, sınıfın üst kısımlarına veya sonuna eklenebilir:
    private enum class NotificationReason {
        PRICE_DROP,
        PRICE_INCREASE,
        NONE
    }

    private data class LegNotificationInfo(
        val reason: NotificationReason = NotificationReason.NONE,
        val messagePart: String? = null
    )

    
}
