package kr.heeblings.api.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import com.google.cloud.Timestamp
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

// 식물 리스트 조회 응답 DTO
// 식물 리스트 조회 응답 DTO
data class PlantListResponse(
    val plantId: String, // Long -> String 변경 (Firestore ID)
    val nickname: String?,
    val imageUrl: String?,
    val imageStatus: String?,
    val startDate: LocalDate,
    val dDay: Long,
    val lastWateredDate: LocalDate?,
    val nextWateringDate: LocalDate?,
    val nextWateringDDay: Long?,
    val isWateringNeeded: Boolean,
    val lastRepottedDate: LocalDate?,
    val plantType: String?
) {
    companion object {
        fun fromFirestoreMap(data: Map<String, Any?>): PlantListResponse {
            val today = LocalDate.now()

            val startDate = toLocalDate(data["startDate"]) ?: LocalDate.now()
            val lastWateredDate = toLocalDate(data["lastWateredDate"])
            val nextWateringDate = toLocalDate(data["nextWateringDate"])
            val lastRepottedDate = toLocalDate(data["lastRepottedDate"])

            val dDay = java.time.temporal.ChronoUnit.DAYS.between(startDate, today)
            val nextWateringDDay = nextWateringDate?.let { java.time.temporal.ChronoUnit.DAYS.between(today, it) }
            val isWateringNeeded = nextWateringDDay != null && nextWateringDDay <= 0

            return PlantListResponse(
                plantId = data["plantId"] as String,
                nickname = data["nickname"] as? String,
                imageUrl = data["imageUrl"] as? String,
                imageStatus = data["imageStatus"] as? String,
                startDate = startDate,
                dDay = dDay,
                lastWateredDate = lastWateredDate,
                nextWateringDate = nextWateringDate,
                nextWateringDDay = nextWateringDDay,
                isWateringNeeded = isWateringNeeded,
                lastRepottedDate = lastRepottedDate,
                plantType = data["plantType"] as? String
            )
        }

        private fun toLocalDate(obj: Any?): LocalDate? {
            return when (obj) {
                is Timestamp -> obj.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                is Date -> obj.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                else -> null
            }
        }
    }
}

// 식물 상세 정보 응답 DTO
data class PlantDetailResponse(
    val plantId: String, // Long -> String 변경
    val nickname: String?,
    val imageUrl: String?,
    val imageStatus: String?,
    val plantType: String?,
    val startDate: LocalDate,
    val dDay: Long,
    val lastWateredDate: LocalDate?,
    val nextWateringDate: LocalDate?,
    val nextWateringDDay: Long?,
    val isWateringNeeded: Boolean,
    val lastRepottedDate: LocalDate?,
    val wateringCycleDays: Int?,
    val description: String?,
    val careInfo: String?
) {
    companion object {
        fun fromFirestoreMap(data: Map<String, Any?>): PlantDetailResponse {
            val today = LocalDate.now()

            val startDate = toLocalDate(data["startDate"]) ?: LocalDate.now()
            val lastWateredDate = toLocalDate(data["lastWateredDate"])
            val nextWateringDate = toLocalDate(data["nextWateringDate"])
            val lastRepottedDate = toLocalDate(data["lastRepottedDate"])

            val dDay = java.time.temporal.ChronoUnit.DAYS.between(startDate, today)
            val nextWateringDDay = nextWateringDate?.let { java.time.temporal.ChronoUnit.DAYS.between(today, it) }
            val isWateringNeeded = nextWateringDDay != null && nextWateringDDay <= 0

            return PlantDetailResponse(
                plantId = data["plantId"] as String,
                nickname = data["nickname"] as? String,
                imageUrl = data["imageUrl"] as? String,
                imageStatus = data["imageStatus"] as? String,
                plantType = data["plantType"] as? String,
                startDate = startDate,
                dDay = dDay,
                lastWateredDate = lastWateredDate,
                nextWateringDate = nextWateringDate,
                nextWateringDDay = nextWateringDDay,
                isWateringNeeded = isWateringNeeded,
                lastRepottedDate = lastRepottedDate,
                wateringCycleDays = (data["wateringCycleDays"] as? Long)?.toInt(),
                description = data["description"] as? String,
                careInfo = data["careInfo"] as? String
            )
        }

        private fun toLocalDate(obj: Any?): LocalDate? {
            return when (obj) {
                is Timestamp -> obj.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                is Date -> obj.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                else -> null
            }
        }
    }
}

// 식물 정보 수정 요청 DTO
data class PlantUpdateRequest(
    @field:Size(max = 10, message = "애칭은 10자 이내로 입력해주세요.")
    val nickname: String?,
    val imageUrl: String?,
    val plantType: String?,
    val startDate: LocalDate,
    val lastWateredDate: LocalDate?,
    val lastRepottedDate: LocalDate?,

    @field:Size(max = 200, message = "식물 정보는 200자 이내로 입력해주세요.")
    val description: String?,

    @field:Size(max = 200, message = "관리 방법은 200자 이내로 입력해주세요.")
    val careInfo: String?,

    // 이미지 삭제 여부 플래그
    val isImageDeleted: Boolean? = false,
)

// 식물 등록 요청 DTO
data class PlantCreateRequest(
    @field:Size(max = 10, message = "애칭은 10자 이내로 입력해주세요.")
    val nickname: String?,
    val imageUrl: String?,
    val plantType: String?,

    @field:NotNull(message = "시작일은 필수 입력 항목입니다.")
    val startDate: LocalDate,

    val lastWateredDate: LocalDate?,
    val lastRepottedDate: LocalDate?,
    val careInfo: String?,
    val description: String?,
    val wateringCycleDays: Int?
)
