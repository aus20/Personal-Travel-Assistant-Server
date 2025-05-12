package com.aus20.config

import com.aus20.security.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
class SecurityConfig(    
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
) {


    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() } // Disable CSRF (safe for API only)
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) } // Disable session management
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
