package kr.heeblings.api.repository

import kr.heeblings.api.domain.Plant
import kr.heeblings.api.domain.PlantUser
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.*

interface PlantRepository : JpaRepository<Plant, Long> {
    fun findByUserUserId(@Param("userId") userId: Long, pageable: Pageable): Page<Plant>

    @Query("SELECT DISTINCT p.user FROM Plant p WHERE p.nextWateringDate <= :date")
    fun findUsersWithPlantsToWaterToday(@Param("date") date: LocalDate): List<PlantUser>

    // JpaRepository가 기본 제공하는 findById 메서드를 오버라이드하고,
    // @EntityGraph 어노테이션을 추가하여 Eager Loading을 적용합니다.
    @EntityGraph(attributePaths = ["user"])
    override fun findById(plantId: Long): Optional<Plant>
}