package kr.heeblings.api.service

import io.awspring.cloud.s3.S3Template
import kr.heeblings.common.utils.log
import net.coobird.thumbnailator.Thumbnails
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Duration
import java.util.*

@Service
class PlantS3UploadService(
    private val s3Template: S3Template
) {
    @Value("\${s3.bucket}")
    private lateinit var bucketName: String

    private val MAX_WIDTH = 1024

    // 서버에서 이미지 리사이징을 처리하는 로직을 다시 추가합니다.
    fun upload(file: MultipartFile): String {
        val originalFilename = file.originalFilename ?: "image.jpg"
        val extension = originalFilename.substringAfterLast(".", "")
        val randomFileName = "${UUID.randomUUID()}.$extension"

        // 이미지 리사이징 로직
        val resizedImageBytes = ByteArrayOutputStream().use { outputStream ->
            Thumbnails.of(file.inputStream)
                .width(MAX_WIDTH) // 최대 가로 크기를 1024로 제한
                .outputQuality(0.75) // 품질을 75%로 설정하여 용량 최적화
                .toOutputStream(outputStream)
            outputStream.toByteArray()
        }

        // 리사이징된 이미지를 S3에 업로드
        s3Template.upload(
            bucketName,
            randomFileName,
            ByteArrayInputStream(resizedImageBytes)
        )

        return randomFileName // 파일 이름 반환
    }

    // 파일 이름을 받아, 1시간 동안 유효한 Pre-signed URL을 생성합니다.
    fun generatePreSignedUrl(fileName: String): String {
        return s3Template.createSignedGetURL(bucketName, fileName, Duration.ofHours(1)).toString()
    }

    // 파일 이름을 받아 S3에서 해당 객체를 삭제합니다.
    fun delete(fileName: String) {
        try {
            s3Template.deleteObject(bucketName, fileName)
            log.debug("S3에서 이미지 삭제 성공: $fileName")
        } catch (e: Exception) {
            log.error("S3 이미지 삭제 중 에러 발생: ${e.message}")
        }
    }
}