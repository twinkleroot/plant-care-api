package kr.heeblings.api.service

import kr.heeblings.api.domain.PlantPushMessage
import org.springframework.stereotype.Service

@Service
class PlantPushMessageService(
    private val firestoreService: PlantFirestoreService
) {
    // 읽지 않은 최근 메시지 10개 조회
    fun getRecentMessages(userId: Long): List<PlantPushMessage> {
        return firestoreService.findUnreadPushMessages(userId.toString())
    }

    // 메시지 읽음 처리
    fun markAsRead(userId: Long, messageId: String): PlantPushMessage {
        return firestoreService.markPushMessageAsRead(userId.toString(), messageId)
    }

    // 메시지 삭제
    fun deleteMessage(userId: Long, messageId: String) {
        firestoreService.deletePushMessage(userId.toString(), messageId)
    }
}