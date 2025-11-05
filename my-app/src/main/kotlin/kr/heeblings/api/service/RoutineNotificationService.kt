package kr.heeblings.api.service

import com.google.firebase.FirebaseApp
import kr.heeblings.api.config.FirebaseConfig
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import kr.heeblings.api.domain.RoutinePushMessage
import kr.heeblings.api.repository.RoutinePushMessageRepository
import kr.heeblings.common.utils.log
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class RoutineNotificationService(
    private val pushMessageRepository: RoutinePushMessageRepository
) {
    // 루틴 앱 사용자가 구독할 토픽 이름
    private val ROUTINE_TOPIC = "routine_daily_reminder"

    /**
     * 매일 저녁 6시 (18:00 KST)에 루틴 앱 알림 실행
     */
    @Scheduled(cron = "0 0 18 * * *", zone = "Asia/Seoul")
    fun sendDailyRoutineReminder() {
        val today = LocalDate.now()
        log.info("[$today] 루틴 관리 앱 저녁 6시 전체 알림 스케줄러 실행...")

        val title = "💪 퇴근 후 2시간, 잊지 않으셨죠?"
        val body = "오늘의 루틴을 완료하고 하루를 멋지게 마무리해보세요!"

        val notification = Notification.builder()
            .setTitle(title)
            .setBody(body)
            .build()

        // 특정 토큰이 아닌 '토픽'으로 메시지를 보냅니다.
        val message = Message.builder()
            .setNotification(notification)
            .setTopic(ROUTINE_TOPIC)
            .build()

        try {
            // FCM으로 메시지 전송
            val routineFirebaseApp = FirebaseApp.getInstance(FirebaseConfig.ROUTINE_APP_NAME)
            // 해당 앱의 FirebaseMessaging 인스턴스를 가져옵니다.
            val fcm = FirebaseMessaging.getInstance(routineFirebaseApp)
            val response = fcm.send(message)
            log.info("[routine-manager] all notice message send success: Topic=$ROUTINE_TOPIC, MessageID=$response")

            // 발송 내역 로깅
            val pushMessage = RoutinePushMessage(
                topic = ROUTINE_TOPIC,
                title = title,
                body = body
            )
            pushMessageRepository.save(pushMessage)

        } catch (e: Exception) {
            log.error("[routine-manager] all notice message send failed: Topic=$ROUTINE_TOPIC, Error=${e.message}")
        }
    }
}
