package kr.heeblings.api.domain

import java.time.LocalDateTime
import java.time.ZoneId

data class PlantUser(
    val userId: Long = 0,
    val kakaoId: Long,
    var nickname: String?,
    var fcmToken: String?,
    val createdAt: LocalDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul")),
    var updatedAt: LocalDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul")),
)
