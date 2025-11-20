package kr.heeblings.api.controller

import kr.heeblings.api.dto.SystemConfigResponse
import kr.heeblings.api.service.PlantFirestoreService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/plant-app/system")
class PlantSystemController(
    private val firestoreService: PlantFirestoreService
) {
    @GetMapping("/config")
    fun getSystemConfig(): ResponseEntity<SystemConfigResponse> {
        val config = firestoreService.getSystemConfig()
        return ResponseEntity.ok(config)
    }
}