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
import java.time.Duration
import com.aus20.dto.response.SimpleSavedSearchResponseDTO
// import Locale
import java.util.Locale

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

    // mapFlightDtoToEntity için DateTimeFormatter (YYYY-MM-DDTHH:mm:ss)
    private val dtoDateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    // <<<--- BU TANIMLAMALARIN UserFlightSearchService İÇİNDE OLDUĞUNDAN EMİN OLUN ---<<<
    private val inputDateFormatterForSave = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
    private val isoDateFormatterForSave = DateTimeFormatter.ISO_LOCAL_DATE 
    // <<<--- BİTTİ ---<<<

    private fun convertToLocalDateSafe(dateString: String?): LocalDate? {
        if (dateString.isNullOrBlank()) return null
        return try {
            LocalDate.parse(dateString, inputDateFormatterForSave)
        } catch (e: DateTimeParseException) {
            try {
                LocalDate.parse(dateString, isoDateFormatterForSave)
            } catch (e2: DateTimeParseException) {
                logger.error("Kayıt için tarih formatı ayrıştırılamadı: '$dateString'", e2)
                throw IllegalArgumentException("Kayıt için geçersiz tarih formatı: '$dateString'")
            }
        }
    }

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
            val isRoundTripSearch = searchEntity.returnDate != null // <<<--- Gidiş-dönüş burada belirleniyor
            // İlişkili Flight entity'lerini FlightResponseDTO'ya map et
            val flightDTOs = searchEntity.flights.map { flightEntity ->
                val legType: FlightLegType = if (isRoundTripSearch) { 
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
                    originAirportCode = flightEntity.originAirportCode,
                    destinationAirportCode = flightEntity.destinationAirportCode,
                    layoverAirports = flightEntity.layoverAirports?.split(",")?.map { it.trim() } ?: emptyList(),
                    departureTime = flightEntity.departureTime.format(dateTimeFormatter),
                    arrivalTime = flightEntity.arrivalTime.format(dateTimeFormatter),
                    carrier = "${flightEntity.carrierCode} ${flightEntity.flightNumber}",
                    duration = formatDuration(flightEntity.duration),
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

    /* 
    fun saveSearchWithTopFlights(dto: FlightSearchRequestDTO, user: User): SimpleSavedSearchResponseDTO {
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
                println("Unexpected result type from flightDataProvider during saveSearchWithTopFlights: ${flightsResult::class.simpleName} for search id ${savedSearchEntity.id}")
                emptyList()
            }

        }
        


        val flightEntities = top10Flights.map { flightDto ->
            mapFlightDtoToEntity(flightDto, savedSearchEntity) // <<<--- Güncellenmiş mantığı kullanır
        }
    
        if (flightEntities.isNotEmpty()){ // flightEntities boş değilse kaydet
            flightRepository.saveAll(flightEntities)
       }


       // Şimdi SimpleSavedSearchResponseDTO'yu oluşturup döndür
       return SimpleSavedSearchResponseDTO( // <<< --- DEĞİŞİKLİK BURADA: Yeni return ifadesi ---
           searchId = savedSearchEntity.id,
           message = "Search saved successfully",
           origin = savedSearchEntity.origin,
           destination = savedSearchEntity.destination,
           departureDate = savedSearchEntity.departureDate.format(dateFormatter),
           returnDate = savedSearchEntity.returnDate?.format(dateFormatter),
           adults = savedSearchEntity.adults
       )    
       
   }
   */

   /*

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

        val newflightEntities = topFlights.map { flightDto ->
            mapFlightDtoToEntity(flightDto, updatedSearchEntity) // <<<--- Güncellenmiş mantığı kullanır
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
    */
    
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
                maxPrice = search.maxPrice, // Sağlayıcıya gönderilecek, sağlayıcı filtreleyecek
                adults = search.adults,
                preferredAirlines = search.preferredAirlines?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            )

            //val newResultsFromProvider = flightDataProvider.searchFlightsWithFilters(requestDto)
            val newApiFlights: List<FlightResponseDTO> = flightDataProvider.searchFlightsWithFilters(requestDto)

            val savedFlightsCopy = search.flights.toList()
            // Önce mevcut (eski) uçuşları bildirim karşılaştırması için yedekle

            // Veritabanını her zaman API'den gelen yeni verilerle güncelle
            search.flights.clear() // Mevcut bağlı uçuşları temizle (orphanRemoval ile silinecekler)
            var dbUpdated = false
            val finalNotificationMessages = mutableListOf<String>()

            logger.info("Search ID ${search.id}: Processing ${newApiFlights.size} new flight offers from provider.")

            // Yeni gelen tüm (limitlenmiş) uçuşları veritabanına kaydetmek için map et
        // mapFlightDtoToEntity, DTO'dan ISO duration'ı hesaplar ve layover'ları string'e çevirir.
        newApiFlights.forEach { flightDto ->
            try {
                search.flights.add(mapFlightDtoToEntity(flightDto, search))
                dbUpdated = true
            } catch (e: DateTimeParseException) {
                logger.error("Error parsing date for flight DTO during periodic search (Search ID ${search.id}): ${e.message}", e)
            }
        }

        if (dbUpdated) {
            userFlightSearchRepository.save(search)
            logger.info("Search ID ${search.id}: DB updated with ${search.flights.size} new flights.")
        } else {
            logger.info("Search ID ${search.id}: No new valid flights from API to update DB.")
        }

        // Bildirim mantığı:
        // Orijinal aramanın gidiş-dönüş olup olmadığını kontrol et
        if (search.returnDate != null) { // Gidiş-Dönüş aramasıydı
            val (eskiGidis, eskiDonus) = partitionSavedFlights(savedFlightsCopy, search.origin, search.destination)

            // Yeni gelen birleşik listeden gidiş ve dönüş bacaklarını ayır
            // FlightService/MockService'ten gelen listenin zaten fiyata göre sıralı olduğunu varsayıyoruz
            // (veya en azından her bacak kendi içinde sıralı ve sonra birleştirilmiş).
            // Bildirim için en ucuz birkaç tanesini karşılaştırmak yeterli olabilir.
            val newApiDepartureFlights = newApiFlights.filter { it.leg == FlightLegType.DEPARTURE }.take(10) // Örnek: En ucuz 10 gidiş
            val newApiReturnFlights = newApiFlights.filter { it.leg == FlightLegType.RETURN }.take(10)    // Örnek: En ucuz 10 dönüş

            val gidisNotifInfo = determineNotificationForLeg(
                eskiGidis, newApiDepartureFlights, "gidiş", search.origin, search.destination
            )
            gidisNotifInfo.messagePart?.let { finalNotificationMessages.add(it) }

            val donusNotifInfo = determineNotificationForLeg(
                eskiDonus, newApiReturnFlights, "dönüş", search.destination, search.origin
            )
            donusNotifInfo.messagePart?.let { finalNotificationMessages.add(it) }

        } else { // Tek Yönlü aramaydı
            // Yeni gelen listenin tamamı tek yön uçuşlarıdır (veya sadece gidiş ayağıdır).
            // FlightService/MockService'ten gelen listenin fiyata göre sıralı olduğunu varsayıyoruz.
            val newApiOneWayFlights = newApiFlights.take(20) // Örnek: En ucuz 20 tek yön

            val tekYonNotifInfo = determineNotificationForLeg(
                savedFlightsCopy, newApiOneWayFlights, "uçuş", search.origin, search.destination
            )
            tekYonNotifInfo.messagePart?.let { finalNotificationMessages.add(it) }
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

    /**
     * Kullanıcının arama kriterlerini ve bulunan en ucuz uçuşu (eğer varsa) kaydeder.
     * Eğer aynı arama kriterleri daha önce kaydedilmişse işlem yapmaz (veya mevcutu döner).
     */
    @Transactional
    fun saveSearchAndInitialFlight(
        dto: FlightSearchRequestDTO,
        user: User,
        fetchedFlights: List<FlightResponseDTO> // FlightController'dan gelen, o anki arama sonuçları
    ): UserFlightSearch? {

        val departureDateParsed = convertToLocalDateSafe(dto.departureDate)
            ?: throw IllegalArgumentException("Gidiş tarihi (departureDate) boş veya geçersiz formatta.")
        val returnDateParsed = convertToLocalDateSafe(dto.returnDate)

        // 1. Bu kriterlerle bir arama zaten var mı diye kontrol et
        val existingSearch = userFlightSearchRepository.findByUserAndOriginAndDestinationAndDepartureDateAndReturnDate(
            user,
            dto.origin,
            dto.destination,
            departureDateParsed,
            returnDateParsed
        )

        if (existingSearch != null) {
            logger.info("Search criteria already exists for user ${user.email} with ID ${existingSearch.id}. No new record created.")
            // Eğer mevcut aramanın hiç uçuşu yoksa ve yeni sonuçlarda uçuş varsa, en ucuzunu ekleyebiliriz.
            // Bu, executePeriodicSearches'in ilk karşılaştırması için faydalı olur.
            if (existingSearch.flights.isEmpty() && fetchedFlights.isNotEmpty()) {
                val cheapestFlightDto = fetchedFlights.minByOrNull { it.price }
                if (cheapestFlightDto != null) {
                    val flightEntity = mapFlightDtoToEntity(cheapestFlightDto, existingSearch)
                    // flightRepository.save(flightEntity) // Eğer CascadeType.ALL yoksa bu gerekli
                    existingSearch.flights.add(flightEntity) // Listeye ekle
                    userFlightSearchRepository.save(existingSearch) // Değişikliği kaydet (yeni uçuşla)
                    logger.info("Added cheapest flight to existing search ID ${existingSearch.id} which had no flights.")
                }
            }
            return existingSearch // Mevcut aramayı döndür
        }

        // 2. Yeni UserFlightSearch entity'si oluştur (henüz uçuşlar eklenmedi)
        val newSearchEntity = UserFlightSearch(
            user = user,
            origin = dto.origin,
            destination = dto.destination,
            departureDate = departureDateParsed,
            returnDate = returnDateParsed,
            maxPrice = dto.maxPrice, // DTO'dan olduğu gibi
            adults = dto.adults,
            preferredAirlines = dto.preferredAirlines?.joinToString(","), // DTO'dan olduğu gibi
            createdAt = LocalDateTime.now(),
            flights = mutableListOf() // Başlangıçta boş liste
        )
        // Önce arama kriterlerini kaydet (ID alması için)
        val savedSearchBase = userFlightSearchRepository.save(newSearchEntity)
        logger.info("New search criteria saved for user ${user.email} with ID ${savedSearchBase.id}")

        // 3. En ucuz uçuşu bul ve kaydet (eğer varsa)
        if (fetchedFlights.isNotEmpty()) {
            val cheapestFlightDto = fetchedFlights.minByOrNull { it.price }
            if (cheapestFlightDto != null) {
                val flightEntity = mapFlightDtoToEntity(cheapestFlightDto, savedSearchBase)
                // flightRepository.save(flightEntity) // Eğer UserFlightSearch'te CascadeType.ALL yoksa
                savedSearchBase.flights.add(flightEntity) // Listeye ekle
                val finalSavedSearch = userFlightSearchRepository.save(savedSearchBase) // Uçuşla birlikte tekrar kaydet
                logger.info("Cheapest flight (Price: ${flightEntity.price}) saved and associated with new search ID ${finalSavedSearch.id}")
                return finalSavedSearch
            } else {
                logger.info("No cheapest flight found in fetched results for new search ID ${savedSearchBase.id}")
            }
        } else {
            logger.info("No flights in the fetched results to save for new search ID ${savedSearchBase.id}")
        }
        return savedSearchBase // Uçuş eklenmemiş olsa bile kaydedilmiş arama kriterlerini dön
    }
    

    // mapFlightDtoToEntity yardımcı fonksiyonu (UserFlightSearchService içinde private):
    private fun mapFlightDtoToEntity(flightDto: FlightResponseDTO, search: UserFlightSearch): Flight {
        // Bu fonksiyon, DateTimeParseException hatasını fırlatabilir, çağıran yer try-catch yapmalı.
        // Ya da bu fonksiyonun içinde try-catch yapılıp null dönülebilir ve çağıran yer filterNotNull kullanabilir.
        // Yukarıdaki ana kodda çağıran yerde try-catch yaptım.
        return Flight(
            origin = flightDto.origin,
            destination = flightDto.destination,
            originAirportCode = flightDto.originAirportCode,
            destinationAirportCode = flightDto.destinationAirportCode,
            layoverAirports = flightDto.layoverAirports.takeIf { it.isNotEmpty() }?.joinToString(","),
            departureTime = LocalDateTime.parse(flightDto.departureTime, dateTimeFormatter),
            arrivalTime = LocalDateTime.parse(flightDto.arrivalTime, dateTimeFormatter),
            carrierCode = flightDto.carrier.split(" ").firstOrNull() ?: flightDto.carrier,
            flightNumber = flightDto.carrier.split(" ").getOrNull(1) ?: "",
            duration = formatDuration(flightDto.duration),
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


