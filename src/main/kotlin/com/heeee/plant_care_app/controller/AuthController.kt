package com.heeee.plant_care_app.controller

import com.heeee.plant_care_app.dto.AuthResponse
import com.heeee.plant_care_app.dto.KakaoLoginRequest
import com.heeee.plant_care_app.service.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService
) {
    @PostMapping("/kakao")
    fun kakaoLogin(@RequestBody request: KakaoLoginRequest): ResponseEntity<AuthResponse> {
        val authResponse = authService.kakaoLogin(request)
        return ResponseEntity.ok(authResponse)
    }
}