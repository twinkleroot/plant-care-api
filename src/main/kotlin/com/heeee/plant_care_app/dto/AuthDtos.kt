package com.heeee.plant_care_app.dto

import com.fasterxml.jackson.annotation.JsonProperty

// --- Request DTOs ---
data class KakaoLoginRequest(val accessToken: String)

// --- Response DTOs ---
data class AuthResponse(
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