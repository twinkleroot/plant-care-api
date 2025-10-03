package com.heeee.plant_care_app.dto

import jakarta.validation.constraints.NotNull
import java.time.LocalDate

// 식물 리스트 조회 응답 DTO
data class PlantListResponse(
    val plantId: Long,
    val nickname: String?,
    val imageUrl: String?,
    val startDate: LocalDate,
    val dDay: Long, // 키우기 시작한 지 며칠 됐는지
    val lastWateredDate: LocalDate,
    val nextWateringDate: LocalDate?, // 다음에 물 줘야 할 날짜
    val nextWateringDDay: Long?, // 물 주기까지 남은 날
    val isWateringNeeded: Boolean, // 오늘 물을 줘야 하는지 여부 (UI 깜빡임 효과용)
    val lastRepottedDate: LocalDate?
)

// 식물 상세 조회 응답 DTO
data class PlantDetailResponse(
    val plantId: Long,
    val nickname: String?,
    val imageUrl: String?,
    val plantType: String?,
    val startDate: LocalDate,
    val dDay: Long,
    val lastWateredDate: LocalDate,
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
    val nickname: String?,
    val imageUrl: String?,
    val plantType: String?,
    val startDate: LocalDate?,
    val lastWateredDate: LocalDate?,
    val lastRepottedDate: LocalDate?
)

// 식물 등록 요청 DTO
data class PlantCreateRequest(
    val nickname: String?,
    val imageUrl: String?,
    val plantType: String?,

    @field:NotNull(message = "시작일은 필수 입력 항목입니다.")
    val startDate: LocalDate,

    val lastWateredDate: LocalDate?, // null일 경우 등록일로 자동 설정
    val lastRepottedDate: LocalDate?,
    val careInfo: String?,
    val description: String?,
    val wateringCycleDays: Int?
)
