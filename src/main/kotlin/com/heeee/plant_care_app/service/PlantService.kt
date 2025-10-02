package com.heeee.plant_care_app.service

import com.heeee.plant_care_app.dto.PlantListResponse
import com.heeee.plant_care_app.repository.PlantRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
@Transactional(readOnly = true) // 기본적으로 읽기 전용 트랜잭션으로 설정
class PlantService(private val plantRepository: PlantRepository) {
    fun getPlantList(userId: Long, pageable: Pageable): Page<PlantListResponse> {
        val plantPage = plantRepository.findByUserUserId(userId, pageable)
        val today = LocalDate.now()

        return plantPage.map { plant ->
            // D-day 계산 로직
            val dDay = ChronoUnit.DAYS.between(plant.startDate, today)

            val nextWateringDate = plant.wateringCycleDays?.let { plant.lastWateredDate.plusDays(it.toLong()) }
            val nextWateringDDay = nextWateringDate?.let { ChronoUnit.DAYS.between(today, it) }

            // 오늘 물을 줘야 하는지 확인 (D-day가 0이거나 과거일 경우)
            val isWateringNeeded = nextWateringDDay != null && nextWateringDDay <= 0

            // Entity -> DTO 변환
            PlantListResponse(
                plantId = plant.plantId,
                nickname = plant.nickname,
                imageUrl = plant.imageUrl,
                startDate = plant.startDate,
                dDay = dDay,
                lastWateredDate = plant.lastWateredDate,
                nextWateringDate = nextWateringDate,
                nextWateringDDay = nextWateringDDay,
                isWateringNeeded = isWateringNeeded,
                lastRepottedDate = plant.lastRepottedDate
            )
        }
    }
}