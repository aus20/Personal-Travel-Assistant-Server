package com.aus20.domain

import jakarta.persistence.*

@Entity // This annotation tells Spring that this class is a JPA entity
@Table(name = "users") // database table name will be "users"
// forces table name to be "users"
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false)
    val password: String,

    @Column(nullable = false)
    val name: String,
    
    @Column(nullable = true)
    var fcmToken: String? = null
)