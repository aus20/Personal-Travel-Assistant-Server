package com.aus20.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.beans.factory.annotation.Value

@ConfigurationProperties(prefix = "amadeus")
class AmadeusPropHolder {
    @Value("\${amadeus.api-key}")
    lateinit var apiKey: String

    @Value("\${amadeus.api-secret}")
    lateinit var apiSecret: String
}