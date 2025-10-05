package kr.heeblings.api.repository

import kr.heeblings.api.domain.PlantTypeWiki
import org.springframework.data.jpa.repository.JpaRepository

interface PlantTypeWikiRepository : JpaRepository<PlantTypeWiki, Long> {
    fun findByPlantTypeName(plantTypeName: String): PlantTypeWiki?
}