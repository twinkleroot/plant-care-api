package kr.heeblings.api.service

import io.awspring.cloud.s3.S3Template
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.time.Duration
import java.util.*

@Service
class PlantS3UploadService(
    private val s3Template: S3Template
) {
    @Value("\${s3.bucket}")
    private lateinit var bucketName: String

    // 이 메서드는 이제 URL 대신, S3에 저장된 '파일 이름(Key)'을 반환합니다.
    fun upload(file: MultipartFile): String {
        val originalFilename = file.originalFilename ?: "image.jpg"
        val extension = originalFilename.substringAfterLast(".", "")
        val randomFileName = "${UUID.randomUUID()}.$extension"

        // 이미지 리사이징 로직을 제거하고, 받은 파일을 그대로 업로드합니다.
        s3Template.upload(
            bucketName,
            randomFileName,
            file.inputStream
        )

        return randomFileName // URL 대신 파일 이름 반환
    }

    // 파일 이름을 받아, 1시간 동안 유효한 Pre-signed URL을 생성합니다.
    fun generatePreSignedUrl(fileName: String): String {
        return s3Template.createSignedGetURL(bucketName, fileName, Duration.ofHours(1)).toString()
    }

    // 파일 이름을 받아 S3에서 해당 객체를 삭제합니다.
    fun delete(fileName: String) {
        try {
            s3Template.deleteObject(bucketName, fileName)
            println("S3에서 이미지 삭제 성공: $fileName")
        } catch (e: Exception) {
            println("S3 이미지 삭제 중 에러 발생: ${e.message}")
        }
    }
}