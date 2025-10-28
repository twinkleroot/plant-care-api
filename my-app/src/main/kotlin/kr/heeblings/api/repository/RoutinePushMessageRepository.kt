package kr.heeblings.api.repository

import kr.heeblings.api.domain.RoutinePushMessage
import org.springframework.data.jpa.repository.JpaRepository

interface RoutinePushMessageRepository : JpaRepository<RoutinePushMessage, Long>
