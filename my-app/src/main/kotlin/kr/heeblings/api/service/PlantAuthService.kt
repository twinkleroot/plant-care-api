package kr.heeblings.api.service

import kr.heeblings.api.repository.PlantUserRepository
import kr.heeblings.common.config.JwtTokenProvider
import kr.heeblings.api.domain.PlantUser
import kr.heeblings.api.dto.KakaoUserInfoResponse
import kr.heeblings.api.dto.PlantAuthResponse
import kr.heeblings.api.dto.PlantKakaoLoginRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient

@Service
class PlantAuthService(
    private val plantUserRepository: PlantUserRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val webClient: WebClient
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
                    fcmToken = null
                )
            )
        } else { // 기존 회원 로그인
            user.nickname = userInfo.properties.nickname // 닉네임 변경 시 업데이트
        }

        val appToken = jwtTokenProvider.generateToken(user.userId)
        return PlantAuthResponse(appToken = appToken, userId = user.userId, nickname = user.nickname)
    }

    private fun getKakaoUserInfo(accessToken: String): KakaoUserInfoResponse {
        return webClient.get()
            .uri("$kakaoApiUrl/v2/user/me")
            .header("Authorization", "Bearer $accessToken")
            .retrieve()
            .bodyToMono(KakaoUserInfoResponse::class.java)
            .block() ?: throw RuntimeException("카카오 사용자 정보를 가져오는데 실패했습니다.")
    }

    @Transactional
    fun handleKakaoUnlink(kakaoId: Long) {
        // 카카오 ID로 사용자를 찾습니다.
        val user = plantUserRepository.findByKakaoId(kakaoId)

        // 사용자가 존재하면 DB에서 삭제합니다.
        // DB 스키마에서 Users와 Plants 테이블이 ON DELETE CASCADE로 연결되어 있으므로,
        // user 레코드가 삭제되면 관련된 모든 plant 레코드도 자동으로 함께 삭제됩니다.
        user?.let {
            plantUserRepository.delete(it)
            println("사용자(kakaoId: $kakaoId)의 연결 해제로 인한 데이터 삭제 완료.")
        } ?: println("연결 해제 요청: 사용자(kakaoId: $kakaoId)를 찾을 수 없음.")
    }
}