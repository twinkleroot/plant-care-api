package kr.heeblings.api.service

import com.fasterxml.jackson.databind.ObjectMapper
import kr.heeblings.api.repository.PlantUserRepository
import kr.heeblings.common.config.JwtTokenProvider
import kr.heeblings.api.domain.PlantUser
import kr.heeblings.api.dto.KakaoUserInfoResponse
import kr.heeblings.api.dto.PlantAuthResponse
import kr.heeblings.api.dto.PlantKakaoLoginRequest
import kr.heeblings.common.utils.log
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDateTime
import java.util.*


@Service
class PlantAuthService(
    private val plantUserRepository: PlantUserRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper,
) {
    @Value("\${kakao.api.url}")
    private lateinit var kakaoApiUrl: String

    @Transactional
    fun kakaoLogin(request: PlantKakaoLoginRequest): PlantAuthResponse {
        val userInfo = getKakaoUserInfo(request.accessToken)
        var user = plantUserRepository.findByKakaoId(userInfo.id)

        if (user == null) { // 신규 가입
            user = plantUserRepository.save(
                PlantUser(
                    kakaoId = userInfo.id,
                    nickname = userInfo.properties.nickname,
                    fcmToken = request.fcmToken
                )
            )
        } else { // 기존 회원 로그인
            user.nickname = userInfo.properties.nickname // 닉네임 변경 시 업데이트
            user.updatedAt = LocalDateTime.now() // 최근 로그인 시간으로 업데이트
            user.fcmToken = request.fcmToken
        }

        val appToken = jwtTokenProvider.generateToken(user.userId)
        return PlantAuthResponse(appToken = appToken, userId = user.userId, nickname = user.nickname)
    }

    @Transactional
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
                plantUserRepository.findByKakaoId(kakaoId)?.let {
                    plantUserRepository.delete(it)
                    log.info("사용자(kakaoId: $kakaoId)의 연결 해제로 인한 데이터 삭제 완료.")
                } ?: log.info("연결 해제 요청: 사용자(kakaoId: $kakaoId)를 찾을 수 없음.")
            } else {
                log.info("웹훅 페이로드의 events.subject.sub 에서 사용자 ID를 찾을 수 없습니다. Decoded Payload: $decodedPayload")
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