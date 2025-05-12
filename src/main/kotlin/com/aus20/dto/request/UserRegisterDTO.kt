package com.aus20.dto.request

import jakarta.validation.constraints.Email // Email formatı için
import jakarta.validation.constraints.NotBlank // Boş olamaz kontrolü için
import jakarta.validation.constraints.Size   // İsteğe bağlı: şifre uzunluğu için

data class UserRegisterDTO(

    @field:NotBlank(message = "Email cannot be blank") // Null, boş string veya sadece boşluk olamaz
    @field:Email(message = "Email should be in a valid format") // Geçerli email formatında olmalı
    val email: String,

    @field:NotBlank(message = "Password cannot be blank")
    @field:Size(min = 6, message = "Password must be at least 6 characters long") 
    val password: String,

    @field:NotBlank(message = "Name cannot be blank") // Null, boş string veya sadece boşluk olamaz
    val name: String
)