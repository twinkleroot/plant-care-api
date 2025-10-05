package kr.heeblings.api.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class PlantKakaoUnlinkRequest(
    @JsonProperty("user_id") // 카카오가 보내주는 사용자의 고유 ID
    val userId: Long,

    @JsonProperty("referrer_type")
    val referrerType: String
)