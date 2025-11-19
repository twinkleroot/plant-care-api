package kr.heeblings.api.domain

import kr.heeblings.api.dto.PlantCreateRequest
import kr.heeblings.api.dto.PlantWikiResponse
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

// JPA Entity가 아닌 순수 Data Class로 변경
data class Plant(
    val plantId: String? = null, // Firestore ID (String)
    val userId: String,
    var nickname: String?,
    var imageUrl: String?,
    var imageStatus: String? = "COMPLETE",
    var plantType: String?,
    var startDate: LocalDate,
    var lastWateredDate: LocalDate?,
    var lastRepottedDate: LocalDate?,
    var wateringCycleDays: Int?,
    var nextWateringDate: LocalDate?,
    var description: String?,
    var careInfo: String?
) {
    companion object {
        // Firestore에 저장할 Map으로 변환하는 정적 팩토리 메서드
        fun toFirestoreMap(request: PlantCreateRequest, wiki: PlantWikiResponse?, imageStatus: String): Map<String, Any?> {
            val map = mutableMapOf<String, Any?>()

            map["nickname"] = request.nickname
            map["plantType"] = request.plantType
            map["imageStatus"] = imageStatus
            map["imageUrl"] = null // 이미지는 별도 업로드

            // 날짜는 Date 타입으로 변환하여 저장
            map["startDate"] = toDate(request.startDate)
            map["lastWateredDate"] = request.lastWateredDate?.let { toDate(it) }
            map["lastRepottedDate"] = request.lastRepottedDate?.let { toDate(it) }

            // 위키 정보가 있으면 채우기
            if (wiki != null) {
                map["wateringCycleDays"] = wiki.wateringCycleDays
                map["description"] = wiki.description
                map["careInfo"] = wiki.careInfo

                // 다음 물주기 날짜 계산 및 저장
                val nextWatering = calculateNextWateringDate(request.lastWateredDate, wiki.wateringCycleDays)
                map["nextWateringDate"] = nextWatering?.let { toDate(it) }
                // 쿼리 효율을 위해 Timestamp(밀리초) 필드도 추가
                map["nextWateringDateMillis"] = nextWatering?.atStartOfDay(ZoneId.of("Asia/Seoul"))?.toInstant()?.toEpochMilli()
            } else {
                // 직접 입력인 경우 기본값 처리
                map["wateringCycleDays"] = null
                map["description"] = ""
                map["careInfo"] = ""
                map["nextWateringDate"] = null
                map["nextWateringDateMillis"] = null
            }

            map["createdAt"] = Date()
            map["updatedAt"] = Date()

            return map
        }

        fun toDate(localDate: LocalDate): Date {
            return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
        }

        fun calculateNextWateringDate(lastWateredDate: LocalDate?, wateringCycleDays: Int?): LocalDate? {
            return if (lastWateredDate != null && wateringCycleDays != null) {
                lastWateredDate.plusDays(wateringCycleDays.toLong())
            } else {
                null
            }
        }
    }
}