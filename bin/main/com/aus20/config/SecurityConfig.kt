package com.aus20.config

import com.aus20.security.JwtAuthenticationFilter
import com.aus20.exception.ErrorResponse
import com.fasterxml.jackson.databind.ObjectMapper // ObjectMapper için import
import jakarta.servlet.http.HttpServletResponse // HttpServletResponse için import
import org.slf4j.LoggerFactory // Loglama için import
import org.springframework.http.MediaType // MediaType için import
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import java.time.LocalDateTime // LocalDateTime için import

@Configuration
class SecurityConfig(    
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val objectMapper: ObjectMapper //JSON dönüşümü için
) {
    private val logger = LoggerFactory.getLogger(SecurityConfig::class.java)


    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() } // Disable CSRF (safe for API only)
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) } // Disable session management
            // Exception Handling'i burada lambda ile yapılandır:
            .exceptionHandling { exceptions ->
                exceptions
                    // AuthenticationEntryPoint (401 Unauthorized için)
                    .authenticationEntryPoint { request, response, authException ->
                        logger.warn("Lambda Unauthorized error: ${authException.message} for request path: ${request.requestURI}")

                        response.status = HttpServletResponse.SC_UNAUTHORIZED // HTTP 401
                        response.contentType = MediaType.APPLICATION_JSON_VALUE

                        val errorResponse = ErrorResponse(
                            timestamp = LocalDateTime.now(),
                            status = HttpServletResponse.SC_UNAUTHORIZED,
                            error = "Unauthorized",
                            message = authException.message ?: "Authentication required to access this resource.",
                            path = request.requestURI ?: "Unknown path"
                        )
                        // ObjectMapper kullanarak JSON'ı doğrudan response'a yaz
                        objectMapper.writeValue(response.outputStream, errorResponse)
                    }
                    // AccessDeniedHandler (403 Forbidden için)
                    .accessDeniedHandler { request, response, accessDeniedException ->
                        logger.warn("Lambda Access Denied error: ${accessDeniedException.message} for request path: ${request.requestURI}")

                        response.status = HttpServletResponse.SC_FORBIDDEN // HTTP 403
                        response.contentType = MediaType.APPLICATION_JSON_VALUE

                        val errorResponse = ErrorResponse(
                            timestamp = LocalDateTime.now(),
                            status = HttpServletResponse.SC_FORBIDDEN,
                            error = "Forbidden",
                            message = accessDeniedException.message ?: "You do not have permission to access this resource.",
                            path = request.requestURI ?: "Unknown path"
                        )
                        // ObjectMapper kullanarak JSON'ı doğrudan response'a yaz
                        objectMapper.writeValue(response.outputStream, errorResponse)
                    }
            }

            .authorizeHttpRequests {
                it.requestMatchers("/api/users/register", "/api/users/login").permitAll() // Allow register & login
                it.anyRequest().authenticated() // other endpoints require authentication
            }
            .formLogin { it.disable() } // Disable form login
         
        // Add the JWT filter before the UsernamePasswordAuthenticationFilter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

}
