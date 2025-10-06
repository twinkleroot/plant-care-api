package kr.heeblings.api.controller

import kr.heeblings.api.dto.PlantCreateRequest
import kr.heeblings.api.dto.PlantDetailResponse
import kr.heeblings.api.dto.PlantListResponse
import kr.heeblings.api.dto.PlantUpdateRequest
import kr.heeblings.api.service.PlantService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.security.Principal

@RestController
@RequestMapping("/plant-app/plants")
class PlantController(private val plantService: PlantService) {
    @GetMapping
    fun getPlantList(principal: Principal, pageable: Pageable): ResponseEntity<Page<PlantListResponse>> {
        val userId = principal.name.toLong() // JWT 토큰에서 추출한 사용자 ID
//        var userId = 1L; // 테스트용
        val plantList = plantService.getPlantList(userId, pageable)
        return ResponseEntity.ok(plantList)
    }

    @GetMapping("/{plantId}")
    fun getPlantDetail(principal: Principal, @PathVariable plantId: Long): ResponseEntity<PlantDetailResponse> {
        val userId = principal.name.toLong()
//        var userId = 1L; // 테스트용
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
//        var userId = 1L; // 테스트용
        val createdPlant = plantService.createPlant(userId, request, imageFile)
        // 성공적으로 생성되었음을 의미하는 201 Created 상태 코드와 함께 생성된 리소스를 반환합니다.
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPlant)
    }

    @PutMapping("/{plantId}", consumes = ["multipart/form-data"])
    fun updatePlant(
        principal: Principal,
        @PathVariable plantId: Long,
        @RequestPart("request") request: PlantUpdateRequest,
        @RequestPart("image", required = false) imageFile: MultipartFile?
    ): ResponseEntity<PlantDetailResponse> {
        val userId = principal.name.toLong()
//        var userId = 1L; // 테스트용
        val updatedPlant = plantService.updatePlant(userId, plantId, request, imageFile)
        return ResponseEntity.ok(updatedPlant)
    }

    @PutMapping("/{plantId}/water")
    fun waterPlant(principal: Principal, @PathVariable plantId: Long): ResponseEntity<PlantDetailResponse> {
        val userId = principal.name.toLong()
//        var userId = 1L; // 테스트용
        val updatedPlant = plantService.waterPlant(userId, plantId)
        return ResponseEntity.ok(updatedPlant)
    }

    @DeleteMapping("/{plantId}")
    fun deletePlant(principal: Principal, @PathVariable plantId: Long): ResponseEntity<Void> {
        val userId = principal.name.toLong()
//        var userId = 1L; // 테스트용
        plantService.deletePlant(userId, plantId)
        // 성공적으로 삭제되었음을 의미하는 204 No Content 상태 코드를 반환합니다.
        return ResponseEntity.noContent().build()
    }
}