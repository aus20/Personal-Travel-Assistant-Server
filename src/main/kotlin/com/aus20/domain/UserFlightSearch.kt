package com.aus20.domain

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "user_flight_searches")
data class UserFlightSearch(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false)
    var origin: String,

    @Column(nullable = false)
    var destination: String,

    @Column(nullable = false)
    var departureDate: LocalDate,

    var returnDate: LocalDate? = null,

    var maxPrice: Int? = null,

    @OneToMany(mappedBy = "userFlightSearch", cascade = [CascadeType.ALL], orphanRemoval = true)
    var flights: MutableList<Flight> = mutableListOf(),

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var adults: Int = 1,

    @Column(nullable = true)
    var preferredAirlines: String? = null,

    //var isRoundTrip: Boolean? = null
)
