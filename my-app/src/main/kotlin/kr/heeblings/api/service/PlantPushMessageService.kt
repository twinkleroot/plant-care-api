package kr.heeblings.api.service

import kr.heeblings.api.domain.PlantPushMessage
import kr.heeblings.api.repository.PlantPushMessageRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.AccessDeniedException

@Service
@Transactional(readOnly = true)
class PlantPushMessageService(
    private val pushMessageRepository: PlantPushMessageRepository
) {
    // 최근 메시지 10개 조회
    fun getRecentMessages(userId: Long): List<PlantPushMessage> {
        return pushMessageRepository.findTop10ByUserUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
    }

    // 메시지 읽음 처리
    @Transactional
    fun markAsRead(userId: Long, messageId: Long): PlantPushMessage {
        val message = findMyMessage(userId, messageId)
        message.isRead = true
        return message // @Transactional에 의해 자동 저장
    }

    // 메시지 삭제
    @Transactional
    fun deleteMessage(userId: Long, messageId: Long) {
        // 소유권 확인 후 삭제
        val message = findMyMessage(userId, messageId)
        pushMessageRepository.delete(message)
    }

    // 메시지 소유권 확인을 위한 private 메서드
    private fun findMyMessage(userId: Long, messageId: Long): PlantPushMessage {
        val message = pushMessageRepository.findById(messageId)
            .orElseThrow { NoSuchElementException("Message not found with id: $messageId") }
        if (message.user.userId != userId) {
            throw AccessDeniedException("You do not have permission to access this message.")
        }
        return message
    }
}