package com.heeee.plant_care_app.dto

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