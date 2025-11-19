package kr.heeblings.api.service

import com.fasterxml.jackson.databind.ObjectMapper
import kr.heeblings.common.config.JwtTokenProvider
import kr.heeblings.api.dto.KakaoUserInfoResponse
import kr.heeblings.api.dto.PlantAuthResponse
import kr.heeblings.api.dto.PlantKakaoLoginRequest
import kr.heeblings.common.utils.log
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.util.*


@Service
class PlantAuthService(
    private val plantFirestoreService: PlantFirestoreService,
    private val jwtTokenProvider: JwtTokenProvider,
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper,
) {
    @Value("\${kakao.api.url}")
    private lateinit var kakaoApiUrl: String

    fun kakaoLogin(request: PlantKakaoLoginRequest): PlantAuthResponse {
        val userInfo = getKakaoUserInfo(request.accessToken)
        val userIdString = plantFirestoreService.saveOrUpdateUser(userInfo.id, userInfo.properties.nickname, request.fcmToken)
        val userId = userIdString.toLong() // Long 타입으로 반환 (API 응답 형식 유지)

        val appToken = jwtTokenProvider.generateToken(userId)
        return PlantAuthResponse(appToken = appToken, userId = userId, nickname = userInfo.properties.nickname)
    }

    fun handleKakaoUnlink(jwtPayload: String) {
        try {
            val payload = jwtPayload.split('.')[1]
            val decodedPayload = String(Base64.getUrlDecoder().decode(payload))

            @Suppress("UNCHECKED_CAST")
            val payloadMap = objectMapper.readValue(decodedPayload, Map::class.java) as Map<String, Any>

            // 카카오의 중첩된 JSON 구조를 올바르게 탐색합니다.
            val events = payloadMap["events"] as? Map<String, Any>
            val unlinkEvent = events?.get("https://schemas.openid.net/secevent/oauth/event-type/user-unlinked") as? Map<String, Any>
            val subject = unlinkEvent?.get("subject") as? Map<String, Any>
            val kakaoIdString = subject?.get("sub") as? String
            val kakaoId = kakaoIdString?.toLongOrNull()

            if (kakaoId != null) {
                plantFirestoreService.deleteUser(kakaoId.toString())
                log.info("사용자(kakaoId: $kakaoId)의 연결 해제로 인한 데이터 삭제 완료.")
            } else {
                log.warn("웹훅 페이로드의 events.subject.sub 에서 사용자 ID를 찾을 수 없습니다. Decoded Payload: $decodedPayload")
            }
        } catch (e: Exception) {
            log.error("카카오 웹훅 JWT 파싱 실패: ${e.message}")
        }
    }

    private fun getKakaoUserInfo(accessToken: String): KakaoUserInfoResponse {
        return webClient.get()
            .uri("$kakaoApiUrl/v2/user/me")
            .header("Authorization", "Bearer $accessToken")
            .retrieve()
            .bodyToMono(KakaoUserInfoResponse::class.java)
            .block() ?: throw RuntimeException("카카오 사용자 정보를 가져오는데 실패했습니다.")
    }
}