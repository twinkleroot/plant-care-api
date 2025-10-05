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
    @Query(
        value = "SELECT * FROM plants p WHERE p.user_id = :userId",
        countQuery = "SELECT count(*) FROM plants p WHERE p.user_id = :userId",
        nativeQuery = true
    )
    fun findByUserUserId(@Param("userId") userId: Long, pageable: Pageable): Page<Plant>

    @Query(
        value = "SELECT DISTINCT p.user_id FROM plants p WHERE p.next_watering_date = :date",
//        countQuery = "SELECT count(DISTINCT p.user_id) FROM plants p WHERE p.next_watering_date = :date",
        nativeQuery = true
    )
    fun findUsersWithPlantsToWaterToday(@Param("date") date: LocalDate): List<PlantUser>
}