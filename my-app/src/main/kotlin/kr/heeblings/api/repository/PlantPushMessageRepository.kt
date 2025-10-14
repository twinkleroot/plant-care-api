package kr.heeblings.api.repository

import kr.heeblings.api.domain.PlantPushMessage
import org.springframework.data.jpa.repository.JpaRepository

interface PlantPushMessageRepository : JpaRepository<PlantPushMessage, Long> {
    // 읽지 않은(isRead=false) 메시지 중에서, 사용자의 메시지를 최신순으로 10개만 조회
    fun findTop10ByUserUserIdAndIsReadFalseOrderByCreatedAtDesc(userId: Long): List<PlantPushMessage>

    // 읽지 않은 메시지가 존재하는지 확인
    fun existsByUserUserIdAndIsReadFalse(userId: Long): Boolean
}