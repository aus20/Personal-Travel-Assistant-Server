// src/main/kotlin/com/aus20/security/CurrentUserResolver.kt
package com.aus20.security

import com.aus20.domain.User
import com.aus20.repository.UserRepository
import org.springframework.core.MethodParameter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class CurrentUserResolver(
    private val userRepository: UserRepository
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.getParameterAnnotation(CurrentUser::class.java) != null &&
               parameter.parameterType == User::class.java
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: org.springframework.web.bind.support.WebDataBinderFactory?
    ): Any? {
        val authentication = SecurityContextHolder.getContext().authentication
        val email = authentication?.name ?: return null
        println("Current user email: $email")

        val user = userRepository.findByEmail(email)
        if (user == null) {
            println(" No user found for email: $email")
            throw IllegalStateException("User not found for email: $email")
        }

        println("User found: $user")
        return user
    }
}
