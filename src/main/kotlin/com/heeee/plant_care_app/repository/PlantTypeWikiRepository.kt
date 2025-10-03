package com.heeee.plant_care_app.repository

import com.heeee.plant_care_app.domain.PlantTypeWiki
import org.springframework.data.jpa.repository.JpaRepository

interface PlantTypeWikiRepository : JpaRepository<PlantTypeWiki, Long> {
    fun findByPlantTypeName(plantTypeName: String): PlantTypeWiki?
}