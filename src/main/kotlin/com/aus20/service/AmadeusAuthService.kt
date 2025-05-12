package com.aus20.service

import com.aus20.config.AmadeusPropHolder
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.time.Instant

@Service
class AmadeusAuthService(
    private val amadeusProperties: AmadeusPropHolder,
    private val restTemplate: RestTemplate
) {
    private var cachedToken: String? = null
    private var tokenExpiry: Instant = Instant.MIN

    fun getAccessToken(): String {
        val now = Instant.now()
        if (cachedToken != null && now.isBefore(tokenExpiry)) {
            return cachedToken!!
        }

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
        }

        val body = "grant_type=client_credentials" +
                   "&client_id=${amadeusProperties.apiKey}" +
                   "&client_secret=${amadeusProperties.apiSecret}"

        val request = HttpEntity(body, headers)

        val response = restTemplate.postForEntity(
            "https://test.api.amadeus.com/v1/security/oauth2/token",
            request,
            AmadeusTokenResponse::class.java
        )

        val token = response.body?.access_token
            ?: throw IllegalStateException("Access token not found in Amadeus response")

        val expiresIn = response.body?.expires_in ?: 1800 // fallback: 30 minutes
        tokenExpiry = now.plusSeconds(expiresIn.toLong())
        cachedToken = token

        return token
    }

    data class AmadeusTokenResponse(
        val access_token: String,
        val token_type: String,
        val expires_in: Int
    )
}

