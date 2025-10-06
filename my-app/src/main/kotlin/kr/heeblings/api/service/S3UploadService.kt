package kr.heeblings.api.service

import io.awspring.cloud.s3.S3Template
import net.coobird.thumbnailator.Thumbnails
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Duration
import java.util.*

@Service
class S3UploadService(
    private val s3Template: S3Template
) {
    @Value("\${s3.bucket}")
    private lateinit var bucketName: String

    private val MAX_WIDTH = 1024

    fun upload(file: MultipartFile): String {
        val originalFilename = file.originalFilename ?: "image.jpg"
        val extension = originalFilename.substringAfterLast(".", "")
        val randomFileName = "${UUID.randomUUID()}.$extension"

        // 이미지 리사이징
        val resizedImageBytes = ByteArrayOutputStream().use { outputStream ->
            Thumbnails.of(file.inputStream)
                .width(MAX_WIDTH) // 최대 가로 크기를 1024로 제한
                .toOutputStream(outputStream)
            outputStream.toByteArray()
        }

        // 1. 리사이징된 이미지를 S3에 업로드
        s3Template.upload(
            bucketName,
            randomFileName,
            ByteArrayInputStream(resizedImageBytes)
        )

        // 2. 업로드된 객체에 대해 1시간 동안 유효한 Pre-signed URL을 생성하여 반환
        return s3Template.createSignedGetURL(bucketName, randomFileName, Duration.ofHours(1)).toString()
    }

    // 이미지 URL을 받아 파일 이름을 추출하고 S3에서 해당 객체를 삭제합니다.
    fun delete(imageUrl: String) {
        try {
            // Pre-signed URL의 경우 '?' 앞부분의 순수 URL만 필요합니다.
            val pureUrl = imageUrl.substringBefore("?")
            val fileName = pureUrl.substringAfterLast("/")
            s3Template.deleteObject(bucketName, fileName)
            println("S3에서 이미지 삭제 성공: $fileName")
        } catch (e: Exception) {
            println("S3 이미지 삭제 중 에러 발생: ${e.message}")
            // 삭제 실패가 전체 로직을 중단시키지 않도록 예외를 로깅만 합니다.
        }
    }
}