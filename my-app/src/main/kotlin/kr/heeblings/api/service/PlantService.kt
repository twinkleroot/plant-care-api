package kr.heeblings.api.service

import kr.heeblings.api.domain.Plant
import kr.heeblings.api.dto.*
import kr.heeblings.api.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

@Service
class PlantService(
    private val firestoreService: PlantFirestoreService,
    private val plantS3UploadService: PlantS3UploadService,
    private val plantImageProcessingService: PlantImageProcessingService, //️ 비동기 서비스 주입
) {
    fun getPlantTypeWikiList(): List<PlantWikiResponse> {
        return firestoreService.getPlantWikiList()
    }

    fun getPlantList(userId: Long, page: Int, size: Int, sort: String): List<PlantListResponse> {
        val userIdString = userId.toString()

        // sort 문자열 파싱 (예: "plantId,desc" -> field="plantId", isAsc=false)
        val sortParts = sort.split(",")
        val sortField = sortParts.getOrElse(0) { "createdAt" }
        val sortDirection = sortParts.getOrElse(1) { "desc" }
        val isAsc = sortDirection.equals("asc", ignoreCase = true)

        val plantMaps = firestoreService.findPlantList(userIdString, page, size, sortField, isAsc)

        return plantMaps.map { plantMap ->
            val plantListResponse = PlantListResponse.fromFirestoreMap(plantMap)
            val preSignedUrl = plantListResponse.imageUrl?.let { plantS3UploadService.generatePreSignedUrl(it) }
            plantListResponse.copy(imageUrl = preSignedUrl)
        }
    }

    fun getPlantDetail(userId: Long, plantIdString: String): PlantDetailResponse {
        val userIdString = userId.toString()

        // Firestore에서 Plant 문서 조회
        val plantData = firestoreService.findPlantById(userIdString, plantIdString)
            ?: throw ResourceNotFoundException("ID가 ${plantIdString}인 식물을 찾을 수 없습니다.")

        // Firestore Map을 DTO로 변환
        val plantDetail = PlantDetailResponse.fromFirestoreMap(plantData)

        // DB에 저장된 파일 이름(plant.imageUrl)을 사용하여 실시간으로 Pre-signed URL 생성
        val preSignedUrl = plantDetail.imageUrl?.let { plantS3UploadService.generatePreSignedUrl(it) }

        // 나머지 계산 로직은 DTO 내부에서 처리
        return plantDetail.copy(imageUrl = preSignedUrl)
    }

    fun createPlant(userId: Long, request: PlantCreateRequest, imageFile: File?): PlantDetailResponse {
        val userIdString = userId.toString()

        val wiki = request.plantType?.let { firestoreService.findWikiByTypeName(it) }
        val imageStatus = if (imageFile != null) "PROCESSING" else "COMPLETE"

        // 1. Firestore 저장용 Map 생성
        val plantMap = Plant.toFirestoreMap(request, wiki, imageStatus)

        // 2. 저장
        val plantIdString = firestoreService.savePlant(userIdString, plantMap)

        // 3. 이미지 비동기 업로드
        if (imageFile != null) {
            plantImageProcessingService.uploadAndSetImageUrl(userIdString, plantIdString, imageFile)
        }

        // 4. 즉시 등록된 상세 정보를 반환합니다.
        return getPlantDetail(userId, plantIdString)
    }

    fun updatePlant(userId: Long, plantIdString: String, request: PlantUpdateRequest, imageFile: File?): PlantDetailResponse {
        val userIdString = userId.toString()

        // 1. 기존 정보 확인 (존재 여부 및 소유권은 Firestore Path 구조상 자동 확인됨)
        val existingPlant = firestoreService.findPlantById(userIdString, plantIdString)
            ?: throw ResourceNotFoundException("ID가 ${plantIdString}인 식물을 찾을 수 없습니다.")

        val updates = mutableMapOf<String, Any?>()

        // 2. 텍스트 정보 업데이트
        request.nickname?.let { updates["nickname"] = it }
        request.startDate.let { updates["startDate"] = Plant.toDate(it) }
        request.lastWateredDate?.let { updates["lastWateredDate"] = Plant.toDate(it) }
        request.lastRepottedDate?.let { updates["lastRepottedDate"] = Plant.toDate(it) }
        request.description?.let { updates["description"] = it }
        request.careInfo?.let { updates["careInfo"] = it }
        updates["updatedAt"] = Date()

        val newPlantType = request.plantType
        val oldPlantType = existingPlant["plantType"] as? String
        var wateringCycleDays = (existingPlant["wateringCycleDays"] as? Long)?.toInt()

        if (newPlantType != null && newPlantType != oldPlantType) {
            updates["plantType"] = newPlantType

            val wiki = firestoreService.findWikiByTypeName(newPlantType)
            if (wiki != null) {
                wateringCycleDays = wiki.wateringCycleDays
                updates["wateringCycleDays"] = wateringCycleDays
                if (request.description.isNullOrEmpty()) updates["description"] = wiki.description
                if (request.careInfo.isNullOrEmpty()) updates["careInfo"] = wiki.careInfo
            } else {
                // 직접 입력 시 정보 초기화
                wateringCycleDays = null
                updates["wateringCycleDays"] = null
                if (request.description.isNullOrEmpty()) updates["description"] = ""
                if (request.careInfo.isNullOrEmpty()) updates["careInfo"] = ""
            }
        }
        // 4. 다음 물주기 날짜 재계산
        val lastWatered = request.lastWateredDate ?: PlantDetailResponse.fromFirestoreMap(existingPlant).lastWateredDate
        if (lastWatered != null) {
            val nextWatering = Plant.calculateNextWateringDate(lastWatered, wateringCycleDays)
            updates["nextWateringDate"] = nextWatering?.let { Plant.toDate(it) }
            updates["nextWateringDateMillis"] = nextWatering?.atStartOfDay(ZoneId.of("Asia/Seoul"))?.toInstant()?.toEpochMilli()
        }

        // 5. 이미지 처리 로직 (수정됨)
        val oldImageUrl = existingPlant["imageUrl"] as? String

        // 5. 이미지 수정 처리
        if (imageFile != null) {
            // 기존 이미지 삭제 (S3 URL이 있는 경우)
            val oldImageUrl = existingPlant["imageUrl"] as? String
            oldImageUrl?.let { plantS3UploadService.delete(it) }

            updates["imageUrl"] = null
            updates["imageStatus"] = "PROCESSING"

            // 비동기 업로드 시작
            plantImageProcessingService.uploadAndSetImageUrl(userIdString, plantIdString, imageFile)
        }
        // Case B: 이미지를 삭제하겠다고 요청한 경우 (새 이미지 없음)
        else if (request.isImageDeleted == true) {
            // 기존 이미지 삭제
            oldImageUrl?.let { plantS3UploadService.delete(it) }

            // DB 정보 초기화
            updates["imageUrl"] = null
            updates["imageStatus"] = "COMPLETE" // 처리 완료 상태로 설정
        }

        // 6. Firestore 업데이트 실행
        firestoreService.updatePlant(userIdString, plantIdString, updates)

        return getPlantDetail(userId, plantIdString)
    }

    fun waterPlant(userId: Long, plantIdString: String): PlantDetailResponse {
        val userIdString = userId.toString()

        val existingPlant = firestoreService.findPlantById(userIdString, plantIdString)
            ?: throw ResourceNotFoundException("ID가 ${plantIdString}인 식물을 찾을 수 없습니다.")

        val today = LocalDate.now()
        val wateringCycleDays = (existingPlant["wateringCycleDays"] as? Long)?.toInt()
        val nextWatering = Plant.calculateNextWateringDate(today, wateringCycleDays)

        val updates = mapOf(
            "lastWateredDate" to Plant.toDate(today),
            "nextWateringDate" to nextWatering?.let { Plant.toDate(it) },
            "nextWateringDateMillis" to nextWatering?.atStartOfDay(ZoneId.of("Asia/Seoul"))?.toInstant()?.toEpochMilli(),
            "updatedAt" to Date()
        )

        firestoreService.updatePlant(userIdString, plantIdString, updates)

        return getPlantDetail(userId, plantIdString)
    }


    fun deletePlant(userId: Long, plantIdString: String) {
        val userIdString = userId.toString()

        val existingPlant = firestoreService.findPlantById(userIdString, plantIdString)
            ?: throw ResourceNotFoundException("ID가 ${plantIdString}인 식물을 찾을 수 없습니다.")

        // S3 이미지 삭제
        val imageUrl = existingPlant["imageUrl"] as? String
        imageUrl?.let { plantS3UploadService.delete(it) }

        // Firestore 문서 삭제
        firestoreService.deletePlant(userIdString, plantIdString)
    }
}