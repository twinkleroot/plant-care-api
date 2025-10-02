package com.heeee.plant_care_app.controller

import com.heeee.plant_care_app.dto.PlantListResponse
import com.heeee.plant_care_app.service.PlantService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/plants")
class PlantController(private val plantService: PlantService) {
    @GetMapping
    fun getPlantList(pageable: Pageable): ResponseEntity<Page<PlantListResponse>> {
        // TODO: 추후 카카오 로그인 연동 후 실제 사용자 ID를 가져오도록 수정해야 합니다.
        val userId = 1L // 임시 사용자 ID

        val plantList = plantService.getPlantList(userId, pageable)
        return ResponseEntity.ok(plantList)
    }
}