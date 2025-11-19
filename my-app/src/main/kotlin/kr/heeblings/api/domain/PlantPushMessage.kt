package kr.heeblings.api.domain

import java.time.LocalDateTime

// JPA Entity 제거 -> 순수 Data Class
data class PlantPushMessage(
    val messageId: String, // Long -> String (Firestore Document ID)
    val title: String,
    val body: String,
    var isRead: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now()
)