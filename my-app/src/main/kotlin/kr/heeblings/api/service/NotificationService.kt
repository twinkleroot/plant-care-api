package kr.heeblings.api.service

import com.google.firebase.FirebaseApp // import 추가
import kr.heeblings.api.config.FirebaseConfig // Config 클래스 import
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import kr.heeblings.common.utils.log
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class NotificationService (
    private val firestoreService: PlantFirestoreService,
) {
    // 매일 오전 9시 (KST 기준)에 실행
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    // 테스트를 위해 5분마다 실행
//    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Seoul")
    // 테스트를 위해 1분 마다 실행
//    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    fun sendWateringReminders() {
        val today = LocalDate.now()
        log.info("[$today] 물주기 알림 스케줄러 실행...")

        // FirestoreService에서 알림 대상 사용자 목록을 가져옴
        val usersToNotify = firestoreService.findUsersToWaterToday()
        if (usersToNotify.isEmpty()) {
            log.info("알림을 보낼 사용자가 없습니다.")
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
                    // "plantApp" 이름으로 초기화된 FirebaseApp 인스턴스를 가져옵니다.
                    val plantFirebaseApp = FirebaseApp.getInstance(FirebaseConfig.PLANT_APP_NAME)
                    // 해당 앱의 FirebaseMessaging 인스턴스를 가져옵니다.
                    val response = FirebaseMessaging.getInstance(plantFirebaseApp).send(message) // plantApp의 fcm으로 발송
                    log.info("알림 발송 성공: UserID=${user.userId}, MessageID=$response")
                    // 푸시 메시지 기록을 FirestoreService를 통해 저장
                    firestoreService.savePushMessage(user, title, body)
                } catch (e: Exception) {
                    log.error("[plant-care] all notice message send failed: UserID=${user.userId}, Token=$token, Error=${e.message}")
                    // TODO: 실패한 토큰은 DB에서 삭제하는 등의 후처리 로직 추가 가능
                }
            }
        }
    }

    fun sendImageProcessedMessage(userId: String, plantId: String) {
        try {
            val user = firestoreService.findUserById(userId) // 사용자 정보를 Firestore에서 조회

            user?.fcmToken?.let { token ->
                if (token.isBlank()) return

                val message = Message.builder()
                    .putData("type", "IMAGE_PROCESSED") // 메시지 타입
                    .putData("plantId", plantId) // 식물 ID
                    .setToken(token)
                    .build()

                // FirebaseApp.getInstance(FirebaseConfig.PLANT_APP_NAME)을 사용해야 정확한 앱 인스턴스 사용
                val response = FirebaseMessaging.getInstance(FirebaseApp.getInstance(FirebaseConfig.PLANT_APP_NAME)).send(message)
                log.debug("Image process complete, message send success : UserID={}, MessageID={}", userId, response)

            } ?: log.warn("FCM token not found for UserID: {}", userId)

        } catch (e: Exception) {
            log.error("이미지 처리 완료 메시지 발송 실패: UserID={}", userId, e)
        }
    }
}