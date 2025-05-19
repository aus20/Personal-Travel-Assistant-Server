package com.aus20.service

import com.aus20.domain.User
import com.aus20.dto.request.UserRegisterDTO
import com.aus20.dto.request.UserLoginDTO
import com.aus20.dto.response.UserResponseDTO
import com.aus20.dto.response.JwtLoginResponse
import com.aus20.repository.UserRepository
import com.aus20.security.JwtTokenProvider
import org.springframework.stereotype.Service
import org.springframework.security.crypto.password.PasswordEncoder

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider
) {
    // Receives registration data, saves a new User to the database, returns a UserResponseDTO
    fun registerUser(dto: UserRegisterDTO): UserResponseDTO {
        // E-postanın daha önce alınıp alınmadığını kontrol et
        if (userRepository.findByEmail(dto.email) != null) {
            throw IllegalArgumentException("Bu e-posta adresi zaten kayıtlı.")
            // Veya kendi özel istisnanızı fırlatabilirsiniz:
            // throw EmailAlreadyExistsException("Bu e-posta adresi zaten kayıtlı: ${dto.email}")
        }

        val hashedPassword = passwordEncoder.encode(dto.password)
        val user = User(
            email = dto.email,
            password = hashedPassword, 
            name = dto.name
        )
        val savedUser = userRepository.save(user)
        return UserResponseDTO(savedUser.id, savedUser.email, savedUser.name)
    }

    // Receives login data, checks if user exists and password matches, returns UserResponseDTO if successful   
    fun loginUser(dto: UserLoginDTO): JwtLoginResponse? {
        val user = userRepository.findByEmail(dto.email) ?: return null
        if (passwordEncoder.matches(dto.password, user.password)) {
            val token = jwtTokenProvider.generateToken(user.email)
            val userDTO = UserResponseDTO(user.id, user.email, user.name)
            
            return JwtLoginResponse(userDTO, token)
        }
        return null
    }
    /**
     * Kullanıcının FCM token'ını temizler (log out işlemi için).
     * @param user Log out yapan kullanıcı.
     * @return FCM token'ı başarıyla temizlendiyse true, kullanıcı bulunamazsa false.
     */
    fun logoutUser(user: com.aus20.domain.User): Boolean { 
        val existingUser = userRepository.findById(user.id).orElse(null)
        if (existingUser != null) {
            existingUser.fcmToken = null
            userRepository.save(existingUser)
            return true
        }
        return false
    }
}
