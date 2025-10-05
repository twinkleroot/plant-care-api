package kr.heeblings.api.controller

import kr.heeblings.api.dto.PlantKakaoUnlinkRequest
import kr.heeblings.api.service.PlantAuthService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/plant-app/webhooks")
class WebhookController(
    private val authService: PlantAuthService
) {
    @Value("\${kakao.admin-key}")
    private lateinit var kakaoAdminKey: String

    @PostMapping("/kakao/unlink")
    fun kakaoUnlink(
        @RequestHeader("Authorization") authorization: String, // 카카오가 보내주는 인증 헤더
        @RequestBody request: PlantKakaoUnlinkRequest
    ): ResponseEntity<Void> {
        // 요청 헤더에 담긴 어드민 키가 우리 서버에 저장된 키와 일치하는지 확인 (보안)
        val expectedAuthHeader = "KakaoAK $kakaoAdminKey"
        if (authorization != expectedAuthHeader) {
            println("카카오 웹훅 인증 실패: 유효하지 않은 어드민 키입니다.")
            return ResponseEntity.status(401).build() // Unauthorized
        }

        // 인증 성공 시, 사용자 삭제 로직 호출
        authService.handleKakaoUnlink(request.userId)

        // 카카오 서버에 정상적으로 처리되었음을 알림 (200 OK)
        return ResponseEntity.ok().build()
    }
}