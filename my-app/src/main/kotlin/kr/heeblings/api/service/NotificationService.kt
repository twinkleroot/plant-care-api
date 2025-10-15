package kr.heeblings.api.service

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import kr.heeblings.api.domain.PlantPushMessage
import kr.heeblings.api.domain.PlantUser
import kr.heeblings.api.repository.PlantPushMessageRepository
import kr.heeblings.api.repository.PlantRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class NotificationService(
    private val plantRepository: PlantRepository,
    private val pushMessageRepository: PlantPushMessageRepository
) {
    // TODO : 매일 오전 9시 (KST 기준)에 실행
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    // 테스트를 위해 5분마다 실행
//    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Seoul")
    // 1분 마다 실행
//    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    fun sendWateringReminders() {
        val today = LocalDate.now()
        println("[$today] 물주기 알림 스케줄러 실행...")

        val usersToNotify = plantRepository.findUsersWithPlantsToWaterToday(today)
        if (usersToNotify.isEmpty()) {
            println("알림을 보낼 사용자가 없습니다.")
            return
        }

        usersToNotify.forEach { user ->
            user.fcmToken?.let { token ->
                if (token.isBlank()) return@let

                val title = "🪴 식물 물주기 알림"
                val body = "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!"

                val notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build()

                val message = Message.builder()
                    .setNotification(notification)
                    .setToken(token)
                    .build()

                try {
                    val response = FirebaseMessaging.getInstance().send(message)
                    println("알림 발송 성공: UserID=${user.userId}, MessageID=$response")

                    val pushMessage = PlantPushMessage(
                        user = user,
                        title = title,
                        body = body
                    )
                    pushMessageRepository.save(pushMessage)

                } catch (e: Exception) {
                    println("알림 발송 실패: UserID=${user.userId}, Token=$token, Error=${e.message}")
                    // TODO: 실패한 토큰은 DB에서 삭제하는 등의 후처리 로직 추가 가능
                }
            }
        }
    }

    fun sendImageProcessedMessage(user: PlantUser, plantId: Long) {
        user.fcmToken?.let { token ->
            if (token.isBlank()) return@let

            val message = Message.builder()
                .putData("type", "IMAGE_PROCESSED") // 메시지 타입
                .putData("plantId", plantId.toString()) // 식물 ID
                .setToken(token)
                .build()

            try {
                val response = FirebaseMessaging.getInstance().send(message)
                println("이미지 처리 완료 메시지 발송 성공: UserID=${user.userId}, MessageID=$response")
            } catch (e: Exception) {
                println("이미지 처리 완료 메시지 발송 실패: UserID=${user.userId}, Error=${e.message}")
            }
        }
    }
}