package kr.heeblings.api.controller

import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthDocController {

    @GetMapping("/.well-known/assetlinks.json")
    fun getAssetLinksJson(): ResponseEntity<ByteArray> {
        val resource = ClassPathResource("static/assetlinks.json")

        return try {
            val inputStream = resource.inputStream
            val data = inputStream.readBytes()
            inputStream.close()

            ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(data)
        } catch (e: Exception) {
            // 파일을 찾을 수 없거나 읽기 오류 발생 시
            ResponseEntity.notFound().build()
        }
    }
}