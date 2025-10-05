package kr.heeblings.api // ❗️패키지명 변경

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["kr.heeblings.common", "kr.heeblings.api"]) // ❗️스캔 경로 변경
@EnableScheduling
class MyAppApplication

fun main(args: Array<String>) {
    runApplication<MyAppApplication>(*args)
}
