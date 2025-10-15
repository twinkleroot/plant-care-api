package kr.heeblings.api.config

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
            // CSRF 보호 기능을 비활성화하되, 웹훅 경로를 '예외(ignore)' 처리하는 방식을 명시적으로 추가하여
            // 설정이 무시되지 않도록 강제합니다.
            .csrf { csrf ->
                csrf.ignoringRequestMatchers(
                    "/plant-app/auth/**",               // 화초앱 로그인/가입 API
                    "/plant-app/webhooks/**",           // 화초앱 카카오 연결 해제 웹훅
                    "/plant-app/plants/**",             // 화초앱 API
                    "/plant-app/push-messages/**"       // 화초앱 푸시 메시지 API
                )
            }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/plant-app/auth/**",       // 화초앱 로그인/가입 API
                    "/plant-app/webhooks/**",   // 화초앱 카카오 연결 해제 웹훅
                    "/plant-app/privacy",       // 화초앱 개인정보처리방침 경로 허용
                    "/plant-app/data-deletion", // 화초앱 데이터 삭제 안내 경로 허용
                    "/health"                   // 공통 헬스 체크 API
                    // TODO: 나중에 채팅앱이 추가되면 "/chat-app/auth/**" 와 같이 추가
                ).permitAll()
                    .anyRequest().authenticated() // 나머지 모든 요청은 인증 필요
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}
