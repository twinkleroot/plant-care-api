package kr.heeblings.api.controller

import kr.heeblings.api.domain.PlantPushMessage
import kr.heeblings.api.service.PlantPushMessageService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/plant-app/push-messages")
class PlantPushMessageController(
    private val pushMessageService: PlantPushMessageService
) {
    // 최근 10개 메시지 가져오기
    @GetMapping
    fun getRecentMessages(principal: Principal): ResponseEntity<List<PlantPushMessage>> {
        val userId = principal.name.toLong()
        val messages = pushMessageService.getRecentMessages(userId)
        return ResponseEntity.ok(messages)
    }

    // 읽지 않은 메시지 상태 조회
    @GetMapping("/unread-status")
    fun hasUnreadMessages(principal: Principal): ResponseEntity<Map<String, Boolean>> {
        val userId = principal.name.toLong()
        val messages = pushMessageService.getRecentMessages(userId)
        // 최근 메시지 중 읽지 않은 것이 하나라도 있는지 확인
        val hasUnread = messages.isNotEmpty()
        return ResponseEntity.ok(mapOf("hasUnread" to hasUnread))
    }

    // 메시지 확인(읽음) 처리
    @PutMapping("/{messageId}/read")
    fun markAsRead(
        principal: Principal,
        @PathVariable messageId: String
    ): ResponseEntity<PlantPushMessage> {
        val userId = principal.name.toLong()
        val message = pushMessageService.markAsRead(userId, messageId)
        return ResponseEntity.ok(message)
    }

    // 메시지 삭제
    @DeleteMapping("/{messageId}")
    fun deleteMessage(
        principal: Principal,
        @PathVariable messageId: String
    ): ResponseEntity<Void> {
        val userId = principal.name.toLong()
        pushMessageService.deleteMessage(userId, messageId)
        return ResponseEntity.noContent().build()
    }
}