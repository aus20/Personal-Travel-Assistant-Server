// src/main/kotlin/com/aus20/exception/GlobalExceptionHandler.kt
package com.aus20.exception

import jakarta.validation.ConstraintViolationException // Bean validation için
import org.slf4j.LoggerFactory // Loglama için
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException // Spring Security erişim hataları için
import org.springframework.security.core.AuthenticationException // Spring Security kimlik doğrulama hataları için
import org.springframework.web.bind.MethodArgumentNotValidException // @Valid anotasyonu ile DTO validasyonu için
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.client.HttpClientErrorException // RestTemplate kaynaklı HTTP hataları için
import org.springframework.web.context.request.ServletWebRequest // İstek yolunu almak için
import org.springframework.web.context.request.WebRequest
import com.aus20.exception.ErrorResponse
@ControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    // Kendi özel istisnalarınız için handler'lar ekleyebilirsiniz.
    // Örneğin, UserNotFoundException, FlightNotFoundException gibi.

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.warn("Illegal argument: ${ex.message} for request: ${(request as ServletWebRequest).request.requestURI}")
        // Genellikle 404 Not Found veya 400 Bad Request bu duruma uygun olabilir.
        // Mesajın içeriğine göre karar verebilirsiniz. Örneğin "Search not found" ise 404.
        val status = if (ex.message?.contains("not found", ignoreCase = true) == true ||
                         ex.message?.contains("bulunamadı", ignoreCase = true) == true) {
            HttpStatus.NOT_FOUND
        } else {
            HttpStatus.BAD_REQUEST
        }
        return createErrorResponse(status, ex.message, request)
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(ex: IllegalStateException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.error("Illegal state: ${ex.message} for request: ${(request as ServletWebRequest).request.requestURI}", ex)
        // Genellikle 500 Internal Server Error veya bazen 409 Conflict olabilir.
        return createErrorResponse(HttpStatus.CONFLICT, ex.message, request) // Duruma göre 409 Conflict daha uygun olabilir
    }

    @ExceptionHandler(AccessDeniedException::class) // Spring Security @PreAuthorize vb. için
    fun handleAccessDeniedException(ex: AccessDeniedException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.warn("Access denied: ${ex.message} for request: ${(request as ServletWebRequest).request.requestURI}")
        return createErrorResponse(HttpStatus.FORBIDDEN, ex.message ?: "Access Denied", request)
    }

    @ExceptionHandler(AuthenticationException::class) // Spring Security kimlik doğrulama hataları için
    fun handleAuthenticationException(ex: AuthenticationException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.warn("Authentication failed: ${ex.message} for request: ${(request as ServletWebRequest).request.requestURI}")
        return createErrorResponse(HttpStatus.UNAUTHORIZED, ex.message ?: "Authentication Failed", request)
    }


    @ExceptionHandler(MethodArgumentNotValidException::class) // @Valid ile DTO validasyon hataları
    fun handleMethodArgumentNotValid(ex: MethodArgumentNotValidException, request: WebRequest): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        val message = "Validation failed: $errors"
        logger.warn("$message for request: ${(request as ServletWebRequest).request.requestURI}")
        return createErrorResponse(HttpStatus.BAD_REQUEST, message, request)
    }

    @ExceptionHandler(ConstraintViolationException::class) // Parametre (@RequestParam, @PathVariable) validasyon hataları
    fun handleConstraintViolationException(ex: ConstraintViolationException, request: WebRequest): ResponseEntity<ErrorResponse> {
        val errors = ex.constraintViolations.joinToString(", ") { "${it.propertyPath}: ${it.message}" }
        val message = "Constraint violation: $errors"
        logger.warn("$message for request: ${(request as ServletWebRequest).request.requestURI}")
        return createErrorResponse(HttpStatus.BAD_REQUEST, message, request)
    }

    // Amadeus API'sinden (veya diğer dış servislerden) gelebilecek HTTP hataları için
    @ExceptionHandler(HttpClientErrorException::class)
    fun handleHttpClientErrorException(ex: HttpClientErrorException, request: WebRequest): ResponseEntity<ErrorResponse> {
        val statusCode = HttpStatus.resolve(ex.statusCode.value()) ?: HttpStatus.INTERNAL_SERVER_ERROR
        val message = "Error from external service: ${ex.statusCode} - ${ex.statusText}. Response: ${ex.responseBodyAsString}"
        logger.warn("$message for request: ${(request as ServletWebRequest).request.requestURI}")
        // İstemciye tüm yanıtı göstermek yerine daha genel bir mesaj verebiliriz
        val clientMessage = "An error occurred while communicating with an external service (${ex.statusCode})."
        return createErrorResponse(statusCode, clientMessage, request)
    }

    // En genel hata yakalayıcı, diğerleri tarafından yakalanmayan tüm Exception'lar için
    @ExceptionHandler(Exception::class)
    fun handleAllUncaughtException(ex: Exception, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.error("An unexpected error occurred for request: ${(request as ServletWebRequest).request.requestURI}", ex)
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.message ?: "An unexpected internal server error occurred.", request)
    }

    private fun createErrorResponse(httpStatus: HttpStatus, message: String?, request: WebRequest): ResponseEntity<ErrorResponse> {
        val path = if (request is ServletWebRequest) request.request.requestURI else "Unknown path"
        val errorResponse = ErrorResponse(
            status = httpStatus.value(),
            error = httpStatus.reasonPhrase,
            message = message,
            path = path
        )
        return ResponseEntity(errorResponse, httpStatus)
    }
}