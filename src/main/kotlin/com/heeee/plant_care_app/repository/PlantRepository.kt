package com.heeee.plant_care_app.repository

import com.heeee.plant_care_app.domain.Plant
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PlantRepository : JpaRepository<Plant, Long> {
    @Query(
        value = "SELECT * FROM Plants p WHERE p.user_id = :userId",
        countQuery = "SELECT count(*) FROM Plants p WHERE p.user_id = :userId",
        nativeQuery = true
    )
    fun findByUserUserId(@Param("userId") userId: Long, pageable: Pageable): Page<Plant>
}