package kr.heeblings.api.repository

import kr.heeblings.api.domain.Plant
import kr.heeblings.api.domain.PlantUser
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface PlantRepository : JpaRepository<Plant, Long> {
    fun findByUserUserId(@Param("userId") userId: Long, pageable: Pageable): Page<Plant>

    @Query("SELECT DISTINCT p.user FROM Plant p WHERE p.nextWateringDate <= :date")
    fun findUsersWithPlantsToWaterToday(@Param("date") date: LocalDate): List<PlantUser>

}