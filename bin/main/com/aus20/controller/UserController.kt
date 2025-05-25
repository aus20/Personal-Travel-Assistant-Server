package com.aus20.controller

import com.aus20.security.JwtTokenProvider
import com.aus20.dto.request.UserRegisterDTO
import com.aus20.dto.request.UserLoginDTO
import com.aus20.dto.response.UserResponseDTO
import com.aus20.service.UserService
import com.aus20.dto.response.JwtLoginResponse
import org.springframework.http.ResponseEntity
import com.aus20.repository.UserRepository
import java.security.Principal
import org.springframework.web.bind.annotation.*
import com.aus20.dto.request.FcmTokenRequest
import jakarta.validation.Valid // @Valid anotasyonunu import et
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.http.HttpStatus
import com.aus20.domain.User
import com.aus20.security.CurrentUser

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository
) {

    @PostMapping("/register")

    fun register(@Valid @RequestBody registerDTO: UserRegisterDTO): ResponseEntity<UserResponseDTO> {
        // Eğer validasyon başarısız olursa, Spring MethodArgumentNotValidException fırlatır
        // ve bu satıra hiç gelinmez. GlobalExceptionHandler bunu yakalar.
        val user = userService.registerUser(registerDTO)
        return ResponseEntity.ok(user)
    }

    @PostMapping("/login")
    fun login(@RequestBody loginDTO: UserLoginDTO): ResponseEntity<JwtLoginResponse> {
        val user = userService.loginUser(loginDTO)
            ?: return ResponseEntity.status(401).build() // Unauthorized

        val token = jwtTokenProvider.generateToken(user.user.email)
        val userDTO = UserResponseDTO(user.user.id, user.user.email, user.user.name)
        return ResponseEntity.ok(JwtLoginResponse(userDTO, token))
    }
    @PostMapping("/fcm-token")
    fun updateFcmToken(@RequestBody request: FcmTokenRequest, principal: Principal): ResponseEntity<Void> {
        val user = userRepository.findByEmail(principal.name)
            ?: return ResponseEntity.notFound().build()

        user.fcmToken = request.token
        userRepository.save(user)
        return ResponseEntity.ok().build()
    }
    @PostMapping("/logout")
    fun logout(@CurrentUser user: User): ResponseEntity<Map<String, String>> {
        val success = userService.logoutUser(user)
        return if (success) {
            ResponseEntity.ok(mapOf("message" to "User logged out successfully and FCM token cleared."))
        } else {
            // Bu durum pek olası değil çünkü @CurrentUser kullanıcıyı bulamazsa zaten hata verir
            // veya kimlik doğrulama filtresi engeller.
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "User not found for logout."))
        }
    }

}