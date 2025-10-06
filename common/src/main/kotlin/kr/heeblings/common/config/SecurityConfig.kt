package kr.heeblings.common.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/plant-app/auth/**",      // 화초앱 로그인/가입 API
                    "/plant-app/webhooks/**",  // 화초앱 카카오 연결 해제 웹훅
                    "/health"                  // 공통 헬스 체크 API
                    // TODO: 나중에 채팅앱이 추가되면 "/chat-app/auth/**" 와 같이 추가
                ).permitAll()
                    .anyRequest().authenticated() // 나머지 모든 요청은 인증 필요
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}
