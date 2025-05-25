// src/main/kotlin/com/aus20/exception/ErrorResponse.kt
package com.aus20.exception

import java.time.LocalDateTime

/**
 * API hataları için standart bir yanıt yapısı.
 * @param timestamp Hatanın oluştuğu zaman.
 * @param status HTTP durum kodu.
 * @param error HTTP durumunun açıklaması (örn: "Not Found", "Bad Request").
 * @param message Hata hakkında daha detaylı bilgi veya fırlatılan istisnanın mesajı.
 * @param path Hatanın oluştuğu istek yolu.
 */
data class ErrorResponse(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: Int,
    val error: String,
    val message: String?,
    val path: String
)