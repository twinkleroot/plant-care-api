package com.heeee.plant_care_app.service

import com.heeee.plant_care_app.config.JwtTokenProvider
import com.heeee.plant_care_app.domain.User
import com.heeee.plant_care_app.dto.AuthResponse
import com.heeee.plant_care_app.dto.KakaoLoginRequest
import com.heeee.plant_care_app.dto.KakaoUserInfoResponse
import com.heeee.plant_care_app.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val webClient: WebClient
) {
    @Value("\${kakao.api.url}")
    private lateinit var kakaoApiUrl: String

    @Transactional
    fun kakaoLogin(request: KakaoLoginRequest): AuthResponse {
        val userInfo = getKakaoUserInfo(request.accessToken)
        var user = userRepository.findByKakaoId(userInfo.id)

        if (user == null) { // 신규 가입
            user = userRepository.save(
                User(
                    kakaoId = userInfo.id,
                    nickname = userInfo.properties.nickname,
                    fcmToken = null
                )
            )
        } else { // 기존 회원 로그인
            user.nickname = userInfo.properties.nickname // 닉네임 변경 시 업데이트
        }

        val appToken = jwtTokenProvider.generateToken(user.userId)
        return AuthResponse(appToken = appToken, userId = user.userId, nickname = user.nickname)
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