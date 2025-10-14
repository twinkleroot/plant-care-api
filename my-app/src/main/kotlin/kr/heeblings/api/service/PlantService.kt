package kr.heeblings.api.service

import kr.heeblings.api.repository.PlantRepository
import kr.heeblings.api.repository.PlantTypeWikiRepository
import kr.heeblings.api.repository.PlantUserRepository
import jakarta.persistence.EntityNotFoundException
import kr.heeblings.api.domain.Plant
import kr.heeblings.api.dto.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.nio.file.AccessDeniedException
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class PlantService(
    private val plantRepository: PlantRepository,
    private val plantUserRepository: PlantUserRepository,
    private val plantTypeWikiRepository: PlantTypeWikiRepository,
    private val plantS3UploadService: PlantS3UploadService,
) {
    @Transactional(readOnly = true)
    fun getPlantTypeWikiList(): List<PlantTypeWikiResponse> {
        // 모든 위키 데이터를 찾아 DTO로 변환하여 반환
        return plantTypeWikiRepository.findAll().map { wiki ->
            PlantTypeWikiResponse(plantTypeName = wiki.plantTypeName)
        }
    }

    @Transactional(readOnly = true)
    fun getPlantList(userId: Long, pageable: Pageable): Page<PlantListResponse> {
        val plantPage = plantRepository.findByUserUserId(userId, pageable)
        val today = LocalDate.now()

        return plantPage.map { plant ->
            // D-day 계산 로직
            val decisionDay = ChronoUnit.DAYS.between(plant.startDate, today)

            val nextWateringDDay = plant.nextWateringDate?.let { ChronoUnit.DAYS.between(today, it) }

            // 오늘 물을 줘야 하는지 확인 (D-day가 0이거나 과거일 경우)
            val isWateringNeeded = nextWateringDDay != null && nextWateringDDay <= 0

            // 각 식물의 파일 이름을 사용하여 실시간으로 Pre-signed URL 생성
            val preSignedUrl = plant.imageUrl?.let { plantS3UploadService.generatePreSignedUrl(it) }

            // Entity -> DTO 변환
            PlantListResponse(
                plantId = plant.plantId,
                nickname = plant.nickname,
                imageUrl = preSignedUrl,
                startDate = plant.startDate,
                decisionDay = decisionDay,
                lastWateredDate = plant.lastWateredDate,
                nextWateringDate = plant.nextWateringDate,
                nextWateringDDay = nextWateringDDay,
                isWateringNeeded = isWateringNeeded,
                lastRepottedDate = plant.lastRepottedDate
            )
        }
    }

    @Transactional(readOnly = true)
    fun getPlantDetail(userId: Long, plantId: Long): PlantDetailResponse {
        // ID를 기준으로 식물을 찾고, 없으면 예외를 발생시킵니다.
        val plant = plantRepository.findById(plantId)
            .orElseThrow {
                EntityNotFoundException("ID가 ${plantId}인 식물을 찾을 수 없습니다.")
            }

        // 요청한 사용자의 식물이 맞는지 확인하여 다른 사용자의 정보 조회를 방지합니다. (보안)
        if (plant.user.userId != userId) {
            throw AccessDeniedException("해당 식물에 대한 접근 권한이 없습니다.")
        }

        // 리스트 조회 로직과 동일하게 D-day 등 계산
        val today = LocalDate.now()
        val decisionDay = ChronoUnit.DAYS.between(plant.startDate, today)

        val nextWateringDDay = plant.nextWateringDate?.let { ChronoUnit.DAYS.between(today, it) }
        val isWateringNeeded = nextWateringDDay != null && nextWateringDDay <= 0

        // DB에 저장된 파일 이름(plant.imageUrl)을 사용하여 실시간으로 Pre-signed URL 생성
        val preSignedUrl = plant.imageUrl?.let { plantS3UploadService.generatePreSignedUrl(it) }

        // Entity를 상세 응답 DTO로 변환하여 반환
        return PlantDetailResponse(
            plantId = plant.plantId,
            nickname = plant.nickname,
            imageUrl = preSignedUrl,
            plantType = plant.plantType,
            startDate = plant.startDate,
            decisionDay = decisionDay,
            lastWateredDate = plant.lastWateredDate,
            nextWateringDate = plant.nextWateringDate,
            nextWateringDDay = nextWateringDDay,
            isWateringNeeded = isWateringNeeded,
            lastRepottedDate = plant.lastRepottedDate,
            wateringCycleDays = plant.wateringCycleDays,
            description = plant.description,
            careInfo = plant.careInfo
        )
    }

    // 데이터를 생성하므로 @Transactional 어노테이션이 필요합니다.
    @Transactional
    fun createPlant(userId: Long, request: PlantCreateRequest, imageFile: MultipartFile?): PlantDetailResponse {
        // 요청한 사용자를 찾습니다.
        val user = plantUserRepository.findById(userId)
            .orElseThrow { EntityNotFoundException("ID가 ${userId}인 사용자를 찾을 수 없습니다.") }

        // 이미지 파일이 있으면 S3에 업로드하고 URL을 가져옴
        val imageFileName = imageFile?.let { plantS3UploadService.upload(it) }

        // DTO를 바탕으로 새로운 Plant 엔티티를 생성합니다.
        val newPlant = Plant(
            user = user,
            nickname = request.nickname,
            imageUrl = imageFileName,
            plantType = request.plantType,
            startDate = request.startDate,
            lastWateredDate = request.lastWateredDate,
            nextWateringDate = null,
            lastRepottedDate = null,
            wateringCycleDays = null,
            description = null,
            careInfo = null,
        )

        // plantType이 있는 경우, 위키 테이블에서 정보 조회 후 채워넣기
        request.plantType?.let {
            plantTypeWikiRepository.findByPlantTypeName(it)?.let { wiki ->
                newPlant.wateringCycleDays = wiki.wateringCycleDays
                newPlant.description = wiki.description
                newPlant.careInfo = wiki.careInfo
            }
        }

        newPlant.nextWateringDate = calculateNextWateringDate(newPlant.lastWateredDate, newPlant.wateringCycleDays)

        // 생성된 엔티티를 데이터베이스에 저장합니다.
        val savedPlant = plantRepository.save(newPlant)

        // 저장된 식물의 상세 정보를 DTO로 변환하여 반환합니다.
        return getPlantDetail(userId, savedPlant.plantId)
    }

    // 데이터를 수정하므로 @Transactional 어노테이션이 필요합니다.
    @Transactional
    fun updatePlant(userId: Long, plantId: Long, request: PlantUpdateRequest, imageFile: MultipartFile?): PlantDetailResponse {
        // 상세 조회 로직과 동일하게 식물을 찾고, 소유권을 확인합니다.
        val plant = plantRepository.findById(plantId)
            .orElseThrow { EntityNotFoundException("ID가 ${plantId}인 식물을 찾을 수 없습니다.") }

        if (plant.user.userId != userId) {
            throw AccessDeniedException("해당 식물에 대한 접근 권한이 없습니다.")
        }

        // 이미지 파일이 새로 들어오면 기존 이미지를 삭제하고 새 이미지로 대체합니다.
        imageFile?.let { newFile ->
            // 1. 기존 이미지가 있다면 S3에서 삭제
            plant.imageUrl?.let { oldImageFileName ->
                plantS3UploadService.delete(oldImageFileName)
            }
            // 2. 새 이미지를 업로드하고 URL을 업데이트
            plant.imageUrl = plantS3UploadService.upload(newFile)
        }

        // 기본 정보 먼저 업데이트
        request.nickname?.let { plant.nickname = it }
        request.imageUrl?.let { plant.imageUrl = it }
        request.startDate?.let { plant.startDate = it }
        request.lastWateredDate?.let { plant.lastWateredDate = it }
        request.lastRepottedDate?.let { plant.lastRepottedDate = it }

        // plantType이 새로 입력되었거나 변경되었는지 확인
        val oldPlantType = plant.plantType
        val newPlantType = request.plantType
        if (newPlantType != null && newPlantType != oldPlantType) {
            // 위키 테이블에서 식물 정보 조회
            plantTypeWikiRepository.findByPlantTypeName(newPlantType)?.let { wiki ->
                plant.plantType = newPlantType // plantType도 업데이트
                // 조회된 정보로 식물 엔티티 업데이트
                plant.wateringCycleDays = wiki.wateringCycleDays
                plant.description = wiki.description
                plant.careInfo = wiki.careInfo
            } ?: run {
                plant.plantType = newPlantType // plantType은 업데이트하되, 정보는 초기화
                // 위키에 정보가 없는 경우, 기존 정보를 초기화
                plant.wateringCycleDays = null
                plant.description = null
                plant.careInfo = null
            }
        }

        // lastWateredDate나 wateringCycleDays가 변경되었으므로 nextWateringDate를 다시 계산하여 업데이트
        plant.nextWateringDate = calculateNextWateringDate(plant.lastWateredDate, plant.wateringCycleDays)

        // 변경된 내용을 DB에 저장합니다. @Transactional에 의해 메서드 종료 시 자동으로 flush 됩니다.
        // 수정된 결과를 다시 DTO로 변환하여 반환합니다.
        return getPlantDetail(userId, plantId)
    }

    // 데이터를 수정하므로 @Transactional 어노테이션이 필요합니다.
    @Transactional
    fun waterPlant(userId: Long, plantId: Long): PlantDetailResponse {
        // 기존 로직과 동일하게 식물을 찾고, 소유권을 확인합니다.
        val plant = plantRepository.findById(plantId)
            .orElseThrow { EntityNotFoundException("ID가 ${plantId}인 식물을 찾을 수 없습니다.") }

        if (plant.user.userId != userId) {
            throw AccessDeniedException("해당 식물에 대한 접근 권한이 없습니다.")
        }

        // 마지막으로 물 준 날짜를 오늘 날짜로 업데이트합니다.
        plant.lastWateredDate = LocalDate.now()
        // nextWateringDate도 함께 재계산하여 업데이트
        plant.nextWateringDate = calculateNextWateringDate(plant.lastWateredDate, plant.wateringCycleDays)

        // 수정된 결과를 다시 DTO로 변환하여 반환합니다.
        return getPlantDetail(userId, plantId)
    }


    // 데이터를 삭제하므로 @Transactional 어노테이션이 필요합니다.
    @Transactional
    fun deletePlant(userId: Long, plantId: Long) {
        // 수정 로직과 동일하게 식물을 찾고, 소유권을 확인합니다.
        val plant = plantRepository.findById(plantId)
            .orElseThrow { EntityNotFoundException("ID가 ${plantId}인 식물을 찾을 수 없습니다.") }

        if (plant.user.userId != userId) {
            throw AccessDeniedException("해당 식물에 대한 접근 권한이 없습니다.")
        }

        // DB에서 식물 정보를 삭제하기 전에, S3에 이미지가 있다면 먼저 삭제합니다.
        plant.imageUrl?.let { imageFileName ->
            plantS3UploadService.delete(imageFileName)
        }

        // 소유권이 확인되면 ID를 기준으로 식물 데이터를 삭제합니다.
        plantRepository.deleteById(plantId)
    }

    private fun calculateNextWateringDate(lastWateredDate: LocalDate?, wateringCycleDays: Int?): LocalDate? {
        return lastWateredDate?.let { lastDate ->
            wateringCycleDays?.let { cycle ->
                lastDate.plusDays(cycle.toLong())
            }
        }
    }
}