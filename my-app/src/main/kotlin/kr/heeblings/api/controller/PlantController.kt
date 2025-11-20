package kr.heeblings.api.controller

import kr.heeblings.api.service.PlantService
import jakarta.validation.Valid
import kr.heeblings.api.dto.*
import kr.heeblings.api.service.PlantImageProcessingService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.security.Principal

@RestController
@RequestMapping("/plant-app/plants")
class PlantController(
    private val plantService: PlantService,
    private val plantImageProcessingService: PlantImageProcessingService,
) {

    @GetMapping("/plant-types")
    fun getPlantTypes(): ResponseEntity<List<PlantWikiResponse>> {
        val plantTypes = plantService.getPlantTypeWikiList()
        return ResponseEntity.ok(plantTypes)
    }

    @GetMapping
    fun getPlantList(
        principal: Principal,
        @RequestParam(defaultValue = "0") page: Int, // Pageable 대신 Int 사용
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "plantId,desc") sort: String // 정렬 파라미터
    ): ResponseEntity<List<PlantListResponse>> { // Page<> 대신 List<> 반환
        val userId = principal.name.toLong()
        val plantList = plantService.getPlantList(userId, page, size, sort)
        return ResponseEntity.ok(plantList)
    }

    @GetMapping("/{plantId}")
    fun getPlantDetail(
        principal: Principal,
        @PathVariable plantId: String // ❗️ Firestore ID는 String
    ): ResponseEntity<PlantDetailResponse> {
        val userId = principal.name.toLong()
        val plantDetail = plantService.getPlantDetail(userId, plantId)
        return ResponseEntity.ok(plantDetail)
    }

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createPlant(
        principal: Principal,
        @RequestPart("request") @Valid request: PlantCreateRequest,
        @RequestPart("image", required = false) imageFile: MultipartFile?,
    ): ResponseEntity<PlantDetailResponse> {
        val userId = principal.name.toLong()

        // MultipartFile이 있으면 임시 파일로 변환, 없으면 null 전달
        val tempFile = imageFile?.let {
            val file = File.createTempFile("upload_", "_${it.originalFilename}")
            it.transferTo(file)
            file
        }

        val result = plantService.createPlant(userId, request, tempFile)
        return ResponseEntity.ok(result)
    }

    @PutMapping(
        value = ["/{plantId}"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    fun updatePlant(
        principal: Principal,
        @PathVariable plantId: String,
        @RequestPart("request") request: PlantUpdateRequest,
        @RequestPart("image", required = false) imageFile: MultipartFile?,
    ): ResponseEntity<PlantDetailResponse> {
        val userId = principal.name.toLong()

        // MultipartFile이 있으면 임시 파일로 변환
        val tempFile = imageFile?.let {
            val file = File.createTempFile("upload_", "_${it.originalFilename}")
            it.transferTo(file)
            file
        }

        val result = plantService.updatePlant(userId, plantId, request, tempFile)
        return ResponseEntity.ok(result)
    }

    @PutMapping("/{plantId}/water")
    fun waterPlant(
        principal: Principal,
        @PathVariable plantId: String
    ): ResponseEntity<PlantDetailResponse> {
        val userId = principal.name.toLong()
        val result = plantService.waterPlant(userId, plantId)
        return ResponseEntity.ok(result)
    }

    @DeleteMapping("/{plantId}")
    fun deletePlant(
        principal: Principal,
        @PathVariable plantId: String
    ): ResponseEntity<Void> {
        val userId = principal.name.toLong()
        plantService.deletePlant(userId, plantId)
        // 성공적으로 삭제되었음을 의미하는 204 No Content 상태 코드를 반환합니다.
        return ResponseEntity.noContent().build()
    }

    /**
     * 이미지 등록/수정 요청을 받아 비동기로 처리합니다.
     * 클라이언트는 이미지와 함께 식물 ID를 전달합니다.
     */
    @PostMapping(
        value = ["/image-upload/{plantId}"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    fun uploadPlantImage(
        principal: Principal,
        @PathVariable plantId: String, // Firestore ID (String)
        @RequestPart("image") imageFile: MultipartFile
    ): ResponseEntity<Void> {
        val userId = principal.name.toLong().toString() // 카카오 ID (String)

        // 임시 파일 생성 로직 (기존 유지)
        val tempFile = File.createTempFile("upload_", "_${imageFile.originalFilename}")
        imageFile.transferTo(tempFile)

        // 비동기 서비스 호출 (리사이징 및 S3 업로드)
        plantImageProcessingService.uploadAndSetImageUrl(userId, plantId, tempFile)

        // 클라이언트에게 즉시 200 OK 응답을 반환합니다.
        return ResponseEntity.ok().build()
    }
}