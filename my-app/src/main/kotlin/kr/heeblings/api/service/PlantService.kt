package kr.heeblings.api.service

import kr.heeblings.api.repository.PlantRepository
import kr.heeblings.api.repository.PlantTypeWikiRepository
import kr.heeblings.api.repository.PlantUserRepository
import jakarta.persistence.EntityNotFoundException
import kr.heeblings.api.domain.Plant
import kr.heeblings.api.dto.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
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
    private val plantImageProcessingService: PlantImageProcessingService, //️ 비동기 서비스 주입
) {
    @Transactional(readOnly = true)
    fun getPlantTypeWikiList(): List<PlantTypeWikiResponse> {
        // 모든 위키 데이터를 'plantTypeName'을 기준으로 오름차순 정렬하여 조회합니다.
        val sort = Sort.by(Sort.Direction.ASC, "plantTypeName")
        return plantTypeWikiRepository.findAll(sort).map { wiki ->
            PlantTypeWikiResponse(plantTypeName = wiki.plantTypeName)
        }
    }

    @Transactional(readOnly = true)
    fun getPlantList(userId: Long, pageable: Pageable): Page<PlantListResponse> {
        val plantPage = plantRepository.findByUserUserId(userId, pageable)
        val today = LocalDate.now()

        return plantPage.map { plant ->
            val decisionDay = ChronoUnit.DAYS.between(plant.startDate, today)   // D-day 계산

            val nextWateringDDay = plant.nextWateringDate?.let { ChronoUnit.DAYS.between(today, it) }

            // 오늘 물을 줘야 하는지 확인 (D-day가 0이거나 과거일 경우)
            val isWateringNeeded = nextWateringDDay != null && nextWateringDDay <= 0

            // 각 식물의 파일 이름을 사용하여 실시간으로 Pre-signed URL 생성
            val preSignedUrl = plant.imageUrl?.let { plantS3UploadService.generatePreSignedUrl(it) }

            PlantListResponse(
                plantId = plant.plantId,
                nickname = plant.nickname,
                imageUrl = preSignedUrl,
                imageStatus = plant.imageStatus,
                startDate = plant.startDate,
                decisionDay = decisionDay,
                lastWateredDate = plant.lastWateredDate,
                nextWateringDate = plant.nextWateringDate,
                nextWateringDDay = nextWateringDDay,
                isWateringNeeded = isWateringNeeded,
                lastRepottedDate = plant.lastRepottedDate,
                plantType = plant.plantType,
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
            imageStatus = plant.imageStatus,
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

        // DTO를 바탕으로 새로운 Plant 엔티티를 생성합니다.
        // 1. 이미지 URL을 null로 하여 식물 정보를 먼저 DB에 저장하고 ID를 확보합니다.
        val newPlant = Plant(
            user = user,
            nickname = request.nickname,
            imageUrl = null, // 이미지는 나중에 비동기로 업데이트되므로 일단 null로 저장
            imageStatus = if (imageFile != null) "PROCESSING" else "COMPLETE", // 상태 설정
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

        // 2. 이미지 파일이 있다면, 비동기 업로드를 '요청'합니다. (결과를 기다리지 않음)
        imageFile?.let {
            plantImageProcessingService.uploadAndSetImageUrl(savedPlant.plantId, it)
        }

        // 3. 이미지 업로드 완료를 기다리지 않고, 즉시 사용자에게 응답합니다.
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

        // 텍스트/날짜 정보 업데이트 로직
        plant.nickname = request.nickname
        plant.startDate = request.startDate
        plant.lastWateredDate = request.lastWateredDate
        plant.lastRepottedDate = request.lastRepottedDate
        plant.description = request.description
        plant.careInfo = request.careInfo

        val oldPlantType = plant.plantType
        val newPlantType = request.plantType
        if (newPlantType != null && newPlantType != oldPlantType) {
            plantTypeWikiRepository.findByPlantTypeName(newPlantType)?.let { wiki ->
                plant.plantType = newPlantType
                plant.wateringCycleDays = wiki.wateringCycleDays
                // 사용자가 직접 입력한 설명이 없다면 위키 정보로 채움
                if (request.description.isNullOrEmpty()) {
                    plant.description = wiki.description
                }
                if (request.careInfo.isNullOrEmpty()) {
                    plant.careInfo = wiki.careInfo
                }
            } ?: run {
                plant.plantType = newPlantType
                plant.wateringCycleDays = null
                // 직접 입력한 종류는 위키 정보가 없으므로 비워둠
                if (request.description.isNullOrEmpty()) {
                    plant.description = ""
                }
                if (request.careInfo.isNullOrEmpty()) {
                    plant.careInfo = ""
                }
            }
        }
        plant.nextWateringDate = calculateNextWateringDate(plant.lastWateredDate, plant.wateringCycleDays)

        // 이미지 수정이 있는 경우, 비동기 로직 수행
        imageFile?.let { newFile ->
            plant.imageUrl?.let { oldImageFileName ->
                plantS3UploadService.delete(oldImageFileName)
            }
            plant.imageUrl = null
            plant.imageStatus = "PROCESSING"
            plantImageProcessingService.uploadAndSetImageUrl(plantId, newFile)
        }

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