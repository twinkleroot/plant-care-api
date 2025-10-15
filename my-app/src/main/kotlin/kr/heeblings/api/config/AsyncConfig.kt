package kr.heeblings.api.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync

@Configuration
@EnableAsync // 이 어노테이션이 비동기 기능을 활성화합니다.
class AsyncConfig
