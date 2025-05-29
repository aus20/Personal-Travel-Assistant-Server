package com.aus20.dto.response

import com.google.gson.annotations.SerializedName // Eğer Gson kullanıyorsanız ve JSON anahtarını belirtmek isterseniz

data class WrappedFlightSearchResponseDTO(
    @SerializedName("departureFlight") // JSON'da anahtarın "departureFlight" olmasını sağlar
    val departureFlight: List<FlightResponseDTO>
)