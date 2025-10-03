package com.heeee.plant_care_app.controller

import com.heeee.plant_care_app.dto.PlantCreateRequest
import com.heeee.plant_care_app.dto.PlantDetailResponse
import com.heeee.plant_care_app.dto.PlantListResponse
import com.heeee.plant_care_app.dto.PlantUpdateRequest
import com.heeee.plant_care_app.service.PlantService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/plants")
class PlantController(private val plantService: PlantService) {
    @GetMapping
    fun getPlantList(principal: Principal, pageable: Pageable): ResponseEntity<Page<PlantListResponse>> {
        val userId = principal.name.toLong() // JWT 토큰에서 추출한 사용자 ID
        val plantList = plantService.getPlantList(userId, pageable)
        return ResponseEntity.ok(plantList)
    }

    @GetMapping("/{plantId}")
    fun getPlantDetail(principal: Principal, @PathVariable plantId: Long): ResponseEntity<PlantDetailResponse> {
        val userId = principal.name.toLong()
        val plantDetail = plantService.getPlantDetail(userId, plantId)
        return ResponseEntity.ok(plantDetail)
    }

    @PostMapping
    fun createPlant(
        principal: Principal,
        @Valid @RequestBody request: PlantCreateRequest // @Valid로 DTO의 유효성 검사 활성화
    ): ResponseEntity<PlantDetailResponse> {
        val userId = principal.name.toLong()
        val createdPlant = plantService.createPlant(userId, request)
        // 성공적으로 생성되었음을 의미하는 201 Created 상태 코드와 함께 생성된 리소스를 반환합니다.
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPlant)
    }

    @PutMapping("/{plantId}")
    fun updatePlant(
        principal: Principal,
        @PathVariable plantId: Long,
        @RequestBody request: PlantUpdateRequest // 요청 Body의 JSON을 DTO로 변환
    ): ResponseEntity<PlantDetailResponse> {
        val userId = principal.name.toLong()
        val updatedPlant = plantService.updatePlant(userId, plantId, request)
        return ResponseEntity.ok(updatedPlant)
    }

    @PutMapping("/{plantId}/water")
    fun waterPlant(principal: Principal, @PathVariable plantId: Long): ResponseEntity<PlantDetailResponse> {
        val userId = principal.name.toLong()
        val updatedPlant = plantService.waterPlant(userId, plantId)
        return ResponseEntity.ok(updatedPlant)
    }

    @DeleteMapping("/{plantId}")
    fun deletePlant(principal: Principal, @PathVariable plantId: Long): ResponseEntity<Void> {
        val userId = principal.name.toLong()
        plantService.deletePlant(userId, plantId)
        // 성공적으로 삭제되었음을 의미하는 204 No Content 상태 코드를 반환합니다.
        return ResponseEntity.noContent().build()
    }
}