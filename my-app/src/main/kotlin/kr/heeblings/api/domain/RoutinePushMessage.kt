package kr.heeblings.api.domain

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "routine_push_message")
@EntityListeners(AuditingEntityListener::class)
class RoutinePushMessage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    // 어떤 토픽으로 보냈는지 기록
    @Column(nullable = false)
    val topic: String,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false, length = 1000)
    val body: String,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null
)
