package com.aus20.dto.response

import com.aus20.dto.response.UserResponseDTO

data class JwtLoginResponse(
    val user: UserResponseDTO,
    val token: String
)