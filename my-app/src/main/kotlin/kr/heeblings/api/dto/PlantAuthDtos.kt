package kr.heeblings.api.dto

import com.fasterxml.jackson.annotation.JsonProperty

// --- Request DTOs ---
data class PlantKakaoLoginRequest(
    val accessToken: String,
    val fcmToken: String? // 클라이언트에서 토큰 발급에 실패할 경우를 대비해 nullable로 설정
)

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