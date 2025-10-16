package kr.heeblings.api.service

import kr.heeblings.api.repository.PlantRepository
import kr.heeblings.common.utils.log
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.NoSuchElementException

@Service
class PlantImageProcessingService(
    private val plantS3UploadService: PlantS3UploadService,
    private val plantRepository: PlantRepository,
    private val notificationService: NotificationService,
) {

    @Async // 이 메서드는 별도의 스레드에서 비동기적으로 실행됩니다.
    @Transactional
    fun uploadAndSetImageUrl(plantId: Long, file: MultipartFile) {
        try {
            // 1. 시간이 오래 걸리는 리사이징 및 S3 업로드 수행
            val imageFileName = plantS3UploadService.upload(file)

            // findWithUserById 메서드를 사용하여 Plant와 User를 함께 조회합니다.
            val plant = plantRepository.findById(plantId)
                .orElseThrow { NoSuchElementException("Plant not found with id: $plantId") }

            plant.imageUrl = imageFileName
            plant.imageStatus = "COMPLETE"
            log.debug("async image upload success: plantId = $plantId")

            // 이제 plant.user 객체는 완전히 로드된 상태이므로 안전합니다.
            notificationService.sendImageProcessedMessage(plant.user, plantId)
        } catch (e: Exception) {
            plantRepository.findById(plantId).ifPresent { plant ->
                plant.imageStatus = "FAILED" // 상태를 '실패'로 변경
            }
            // 에러를 더 명확하게 로깅합니다.
            log.debug(e.toString())
            log.error("async image upload failed: plantId = $plantId, error: ${e.message}")
        }
    }
}
