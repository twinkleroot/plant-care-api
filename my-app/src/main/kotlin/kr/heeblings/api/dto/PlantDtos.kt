package kr.heeblings.api.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

// 식물 리스트 조회 응답 DTO
data class PlantListResponse(
    val plantId: Long,
    val nickname: String?,
    val imageUrl: String?,
    val imageStatus: String?,
    val startDate: LocalDate,
    val decisionDay: Long, // 키우기 시작한 지 며칠 됐는지
    val lastWateredDate: LocalDate?,
    val nextWateringDate: LocalDate?, // 다음에 물 줘야 할 날짜
    val nextWateringDDay: Long?, // 다음 물 주는 날까지 남은 날
    val isWateringNeeded: Boolean, // 오늘 물을 줘야 하는지 여부 (UI 깜빡임 효과용)
    val lastRepottedDate: LocalDate?,
    val plantType: String?
)

// 식물 상세 조회 응답 DTO
data class PlantDetailResponse(
    val plantId: Long,
    val nickname: String?,
    val imageUrl: String?,
    val imageStatus: String?,
    val plantType: String?,
    val startDate: LocalDate,
    val decisionDay: Long,
    val lastWateredDate: LocalDate?,
    val nextWateringDate: LocalDate?,
    val nextWateringDDay: Long?,
    val isWateringNeeded: Boolean,
    val lastRepottedDate: LocalDate?,
    val wateringCycleDays: Int?,
    val description: String?,
    val careInfo: String?
)

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
    val careInfo: String?
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

data class PlantTypeWikiResponse(
    val plantTypeName: String
)