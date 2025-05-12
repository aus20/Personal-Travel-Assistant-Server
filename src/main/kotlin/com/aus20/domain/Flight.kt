package com.aus20.domain

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.LocalDateTime
import jakarta.persistence.Table
import jakarta.persistence.Column
import jakarta.persistence.ManyToOne
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn

@Entity
@Table(name = "flights")
data class Flight(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val origin: String,

    @Column(nullable = false)
    val destination: String,

    @Column(nullable = false)
    val departureTime: LocalDateTime,

    @Column(nullable = false)
    val arrivalTime: LocalDateTime,

    @Column(nullable = false)
    val carrierCode: String,

    @Column(nullable = false)
    val flightNumber: String,

    @Column(nullable = false)
    val duration: String,

    @Column(nullable = false)
    val aircraftCode: String,

    @Column(nullable = false)
    val cabinClass: String,

    @Column(nullable = false)
    val numberOfStops: Int,

    @Column(nullable = false)
    val price: Double,

    @Column(nullable = false)
    val currency: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_flight_search_id")
    val userFlightSearch: UserFlightSearch? = null
)
