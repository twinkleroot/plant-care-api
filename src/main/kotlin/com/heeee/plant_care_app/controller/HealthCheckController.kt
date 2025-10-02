package com.heeee.plant_care_app.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController // 이 클래스가 REST API 컨트롤러임을 나타냅니다.
class HealthCheckController {

    @GetMapping("/health") // HTTP GET 요청을 '/health' 경로와 매핑합니다.
    fun healthCheck(): String {
        return "OK"
    }
}