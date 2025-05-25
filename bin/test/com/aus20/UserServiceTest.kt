package com.aus20.service

import com.aus20.domain.User
import com.aus20.dto.request.UserRegisterDTO
import com.aus20.dto.response.UserResponseDTO
import com.aus20.repository.UserRepository
import com.aus20.security.JwtTokenProvider // loginUser testi için gerekecek, şimdiden ekleyebiliriz
import io.mockk.every // MockK anahtar fonksiyonu
import io.mockk.mockk // MockK mock oluşturma fonksiyonu
import io.mockk.verify // MockK doğrulama fonksiyonu
import org.junit.jupiter.api.Assertions.* // JUnit 5 assertion'ları
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder
import io.mockk.slot

class UserServiceTest {

    // Mock'lanacak bağımlılıklarımızı burada lateinit var ile tanımlıyoruz
    private lateinit var userRepository: UserRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var jwtTokenProvider: JwtTokenProvider // registerUser için doğrudan kullanılmayacak ama UserService constructor'ında var

    // Test edilecek asıl servisimiz
    private lateinit var userService: UserService

    @BeforeEach // Bu metot, her bir @Test metodundan önce çalışacak
    fun setUp() {
        // Her test öncesinde bağımlılıklarımızı mock'luyoruz (temiz bir başlangıç için)
        userRepository = mockk(relaxed = true) // UserRepository mocku
        passwordEncoder = mockk()
        jwtTokenProvider = mockk() // UserService constructor'ı bunu beklediği için mock'luyoruz

        // UserService'i mock'lanmış bağımlılıklarla örneklendiriyoruz
        userService = UserService(userRepository, passwordEncoder, jwtTokenProvider)
    }

    // --- registerUser Testleri ---

    @Test
    fun `registerUser_whenEmailIsNotTaken_shouldSaveUserAndReturnUserResponseDTO`() {
        // 1. Arrange (Hazırlık Aşaması - Test için gerekli verileri ve mock davranışlarını ayarla)

        // Test için kullanılacak DTO

        val userRegisterDTO = UserRegisterDTO(
            email = "test@example.com",
            password = "password123",
            name = "Test User"
        )

        every { userRepository.findByEmail(userRegisterDTO.email) } returns null

        // passwordEncoder.encode metodu çağrıldığında ne döndüreceğini belirliyoruz
        val expectedHashedPassword = "hashedPassword_abc123"
        every { passwordEncoder.encode(userRegisterDTO.password) } returns expectedHashedPassword

        // userRepository.save metodu çağrıldığında ne döndüreceğini belirliyoruz
        // any() kullanarak User tipinde herhangi bir argümanla çağrıldığında savedUser'ı döndür diyoruz.
        // slot<User>() kullanarak yakalayıp içeriğini de kontrol edebiliriz ama şimdilik basit tutalım.
        val userToSave = User(email = userRegisterDTO.email, password = expectedHashedPassword, name = userRegisterDTO.name)
        // userRepository.save çağrıldığında, kaydedilmiş gibi davranacak bir User objesi dönmeli (id'si set edilmiş)
        val savedUserFromRepo = userToSave.copy(id = 1L) // Örnek bir ID ile
        every { userRepository.save(any<User>()) } returns savedUserFromRepo

        // Beklenen DTO yanıtı
        val expectedUserResponseDTO = UserResponseDTO(
            id = savedUserFromRepo.id,
            email = savedUserFromRepo.email,
            name = savedUserFromRepo.name
        )

        // 2. Act (Eylem Aşaması - Test edilecek metodu çağır)
        val actualUserResponseDTO = userService.registerUser(userRegisterDTO)

        // 3. Assert (Doğrulama Aşaması - Sonuçların beklendiği gibi olup olmadığını kontrol et)
        assertEquals(expectedUserResponseDTO, actualUserResponseDTO, "Dönen UserResponseDTO beklenenle aynı olmalı.")

        // Ek olarak, bağımlılıkların doğru çağrılıp çağrılmadığını da doğrulayabiliriz (verify)
        verify(exactly = 1) { passwordEncoder.encode(userRegisterDTO.password) } // encode tam olarak 1 kez bu şifreyle çağrıldı mı?

        // userRepository.save metodunun çağrıldığını ve argümanının beklediğimiz gibi olduğunu doğrulama
        // capture<User>() ile save metoduna giden User objesini yakalayıp assert edebiliriz:
        val userSlot = slot<User>()
        verify(exactly = 1) { userRepository.save(capture(userSlot)) } // save tam olarak 1 kez çağrıldı mı ve argümanı yakala

        val capturedUser = userSlot.captured
        assertEquals(userRegisterDTO.email, capturedUser.email, "Kaydedilen kullanıcının e-postası doğru olmalı.")
        assertEquals(expectedHashedPassword, capturedUser.password, "Kaydedilen kullanıcının şifresi hashlenmiş olmalı.")
        assertEquals(userRegisterDTO.name, capturedUser.name, "Kaydedilen kullanıcının adı doğru olmalı.")
    }

    // Buraya diğer test metotları (örneğin e-posta zaten alınmışsa ne olur) eklenecek
    // Örneğin: @Test fun `registerUser_whenEmailIsAlreadyTaken_shouldThrowException()` { ... }
}