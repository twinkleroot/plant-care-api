package kr.heeblings.api.service

import kr.heeblings.api.repository.PlantRepository
import kr.heeblings.api.repository.PlantUserRepository
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
class PlantImageProcessingService(
    private val plantS3UploadService: PlantS3UploadService,
    private val plantRepository: PlantRepository,
    private val plantUserRepository: PlantUserRepository,
    private val notificationService: NotificationService,
) {

    @Async // 이 메서드는 별도의 스레드에서 비동기적으로 실행됩니다.
    @Transactional
    fun uploadAndSetImageUrl(userId: Long, plantId: Long, file: MultipartFile) {
        try {
            // 1. 시간이 오래 걸리는 리사이징 및 S3 업로드 수행
            val imageFileName = plantS3UploadService.upload(file)

            val plant = plantRepository.findById(plantId)
                .orElseThrow { NoSuchElementException("Plant not found with id: $plantId") }

            // userId로 PlantUser를 다시 조회하여 Lazy Loading 문제를 해결합니다.
            val user = plantUserRepository.findById(userId)
                .orElseThrow { NoSuchElementException("User not found with id: $userId") }

            plant.imageUrl = imageFileName
            plant.imageStatus = "COMPLETE"
            println("비동기 이미지 업로드 성공: plantId = $plantId")

            // 이제 안전하게 user 객체를 사용하여 알림을 보낼 수 있습니다.
            notificationService.sendImageProcessedMessage(user, plantId)

//            // 2. 작업 완료 후, DB에서 식물을 찾아 imageUrl 필드를 업데이트
//            plantRepository.findById(plantId).ifPresent { plant ->
//                plant.imageUrl = imageFileName
//                plant.imageStatus = "COMPLETE" // 상태를 '완료'로 변경
//                // @Transactional에 의해 메서드 종료 시 자동 저장됩니다.
//                println("비동기 이미지 업로드 성공 및 정보 업데이트 완료: plantId = $plantId")
//
//                // 이미지 처리 완료 알림 발송
//                notificationService.sendImageProcessedMessage(plant.user, plantId)
//            }
        } catch (e: Exception) {
            plantRepository.findById(plantId).ifPresent { plant ->
                plant.imageStatus = "FAILED" // 상태를 '실패'로 변경
            }
            // 에러를 더 명확하게 로깅합니다.
            e.printStackTrace()
            println("비동기 이미지 업로드 실패: plantId = $plantId, error: ${e.message}")
        }
    }
}
