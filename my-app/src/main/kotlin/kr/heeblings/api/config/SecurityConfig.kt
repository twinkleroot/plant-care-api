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
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/",
                    "/church/**",
                    "/migration/**",
                    "/plant-app/auth/**",
                    "/plant-app/webhooks/**",
                    "/plant-app/system/**",
                    "/plant-app/privacy",
                    "/plant-app/data-deletion",
                    "/routine-manager-app/privacy",
                    "/routine-manager-app/account-deletion",
                    "/parent-helper-app/privacy",
                    "/parent-helper-app/account-deletion",
                    "/.well-known/assetlinks.json",
                    "/app-ads.txt",
                    "/robots.txt",
                    "/health"
                ).permitAll()
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}

