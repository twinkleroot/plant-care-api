package kr.heeblings.api.controller

import kr.heeblings.api.dto.PlantAuthResponse
import kr.heeblings.api.dto.PlantKakaoLoginRequest
import kr.heeblings.api.service.PlantAuthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/plant-app/auth") // URL 경로에 앱 식별자 추가
class PlantAuthController(
    private val plantAuthService: PlantAuthService
) {
    @PostMapping("/kakao")
    fun kakaoLogin(@RequestBody request: PlantKakaoLoginRequest): ResponseEntity<PlantAuthResponse> {
        val authResponse = plantAuthService.kakaoLogin(request)
        return ResponseEntity.ok(authResponse)
    }
}
