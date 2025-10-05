package kr.heeblings.api.dto

import com.fasterxml.jackson.annotation.JsonProperty

// --- Request DTOs ---
data class PlantKakaoLoginRequest(val accessToken: String)

// --- Response DTOs ---
data class PlantAuthResponse(
    val appToken: String,
    val userId: Long,
    val nickname: String?
)

data class KakaoUserInfoResponse(
    val id: Long,
    val properties: KakaoUserProperties
)

data class KakaoUserProperties(
    @JsonProperty("nickname") val nickname: String
)