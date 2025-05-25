package com.aus20.repository

import com.aus20.domain.UserFlightSearch
import com.aus20.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import org.springframework.data.jpa.repository.Query

@Repository
interface UserFlightSearchRepository : JpaRepository<UserFlightSearch, Long> {
    fun findAllByUser(user: User): List<UserFlightSearch>

    fun existsByUserAndOriginAndDestinationAndDepartureDateAndReturnDate(
        user: User,
        origin: String,
        destination: String,
        departureDate: LocalDate,
        returnDate: LocalDate?
    ): Boolean

    @Query("SELECT DISTINCT ufs FROM UserFlightSearch ufs LEFT JOIN FETCH ufs.user LEFT JOIN FETCH ufs.flights")
    fun findAllWithUserAndFlights(): List<UserFlightSearch>
}
