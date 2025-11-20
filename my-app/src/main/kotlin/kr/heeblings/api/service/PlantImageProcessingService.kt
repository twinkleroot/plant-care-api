package kr.heeblings.api.service

import kr.heeblings.common.utils.log
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.io.File

@Service
class PlantImageProcessingService(
    private val plantS3UploadService: PlantS3UploadService,
    private val firestoreService: PlantFirestoreService,
    private val notificationService: NotificationService,
) {

    @Async // 이 메서드는 별도의 스레드에서 비동기적으로 실행됩니다.
    fun uploadAndSetImageUrl(userId: String, plantId: String, file: File) {
        try {
            // 1. 시간이 오래 걸리는 리사이징 및 S3 업로드 수행
            val imageFileName = plantS3UploadService.upload(file)

            // FirestoreService를 통해 ImageStatus와 URL 업데이트
            firestoreService.updatePlantImageStatus(userId, plantId, imageFileName, "COMPLETE")
            log.info("비동기 이미지 업로드 성공 및 Firestore 업데이트 완료: plantId = {}", plantId)

            // 사용자 ID(String)를 사용하여 알림 발송
            notificationService.sendImageProcessedMessage(userId, plantId)
        } catch (e: Exception) {
            // FirestoreService를 통해 ImageStatus를 FAILED로 업데이트
            firestoreService.updatePlantImageStatus(userId, plantId, null, "FAILED")
            log.error("비동기 이미지 업로드 실패: plantId = {}, error: {}", plantId, e.message, e)
        } finally {
            // [중요] 비동기 작업이 끝나면(성공하든 실패하든) 서버에 임시로 만든 파일을 반드시 삭제해야 합니다.
            // 삭제하지 않으면 디스크 용량이 가득 찰 수 있습니다.
            if (file.exists()) {
                file.delete()
                log.debug("Temporary file deleted: {}", file.name)
            }
        }
    }
}
