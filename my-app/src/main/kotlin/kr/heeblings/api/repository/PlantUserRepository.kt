package kr.heeblings.api.repository

import kr.heeblings.api.domain.PlantUser
import org.springframework.data.jpa.repository.JpaRepository

interface PlantUserRepository : JpaRepository<PlantUser, Long> {
    fun findByKakaoId(kakaoId: Long): PlantUser?
}