package kr.heeblings.api.controller

import kr.heeblings.api.service.PlantAuthService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/plant-app/webhooks")
class WebhookController(
    private val plantAuthService: PlantAuthService
) {
    @Value("\${kakao.admin-key}")
    private lateinit var kakaoAdminKey: String

    @PostMapping("/kakao/unlink")
    fun kakaoUnlink(
        @RequestHeader(name = "Authorization", required = false) authorization: String?,
        // DTO 대신, 요청 본문 전체를 순수한 문자열로 받습니다.
        @RequestBody body: String
    ): ResponseEntity<Void> {
        // 실제 운영 환경에서는 Authorization 헤더가 반드시 존재해야 합니다.
        if (authorization == null) {
            println("경고: 카카오 웹훅 요청에 Authorization 헤더가 없습니다. 테스트 환경으로 간주하고 처리를 시도합니다.")
        } else {
            // 요청 헤더에 담긴 어드민 키가 우리 서버에 저장된 키와 일치하는지 확인 (보안)
            val expectedAuthHeader = "KakaoAK $kakaoAdminKey"
            if (authorization != expectedAuthHeader) {
                println("카카오 웹훅 인증 실패: 유효하지 않은 어드민 키입니다.")
                return ResponseEntity.status(401).build() // Unauthorized
            }
        }

        // 이제 서비스에 JWT 문자열을 그대로 전달합니다.
        plantAuthService.handleKakaoUnlink(body)

        // 카카오 서버에 정상적으로 처리되었음을 알림 (200 OK)
        return ResponseEntity.ok().build()
    }
}