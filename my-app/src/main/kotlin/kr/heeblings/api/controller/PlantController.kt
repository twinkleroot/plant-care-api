package kr.heeblings.api.controller

import kr.heeblings.api.service.PlantService
import jakarta.validation.Valid
import kr.heeblings.api.dto.*
import kr.heeblings.api.service.PlantImageProcessingService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
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
        @RequestParam(defaultValue = "0") page: Int, // ❗️ Pageable 대신 Int 사용
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<List<PlantListResponse>> { // ❗️ Page<> 대신 List<> 반환
        val userId = principal.name.toLong()
        val plantList = plantService.getPlantList(userId, page, size)
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

    @PostMapping(consumes = ["multipart/form-data"]) // multipart/form-data 타입만 허용
    fun createPlant(
        principal: Principal,
        @RequestPart("request") @Valid request: PlantCreateRequest,
        @RequestPart("image", required = false) imageFile: MultipartFile?
    ): ResponseEntity<PlantDetailResponse> {
        val userId = principal.name.toLong()
        val createdPlant = plantService.createPlant(userId, request, imageFile)
        // 성공적으로 생성되었음을 의미하는 201 Created 상태 코드와 함께 생성된 리소스를 반환합니다.
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPlant)
    }

    @PutMapping("/{plantId}", consumes = ["multipart/form-data"])
    fun updatePlant(
        principal: Principal,
        @PathVariable plantId: String,
        @RequestPart("request") request: PlantUpdateRequest,
        @RequestPart("image", required = false) imageFile: MultipartFile?
    ): ResponseEntity<PlantDetailResponse> {
        val userId = principal.name.toLong()
        val updatedPlant = plantService.updatePlant(userId, plantId, request, imageFile)
        return ResponseEntity.ok(updatedPlant)
    }

    @PutMapping("/{plantId}/water")
    fun waterPlant(
        principal: Principal,
        @PathVariable plantId: String
    ): ResponseEntity<PlantDetailResponse> {
        val userId = principal.name.toLong()
        val updatedPlant = plantService.waterPlant(userId, plantId)
        return ResponseEntity.ok(updatedPlant)
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
    @PostMapping("/image-upload/{plantId}")
    fun uploadPlantImage(
        principal: Principal,
        @PathVariable plantId: String, // Firestore ID (String)
        @RequestPart("image") imageFile: MultipartFile
    ): ResponseEntity<Void> {
        val userId = principal.name.toLong().toString() // 카카오 ID (String)

        // 비동기 서비스 호출 (리사이징 및 S3 업로드)
        plantImageProcessingService.uploadAndSetImageUrl(userId, plantId, imageFile)

        // 클라이언트에게 즉시 200 OK 응답을 반환합니다.
        return ResponseEntity.ok().build()
    }
}