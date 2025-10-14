package kr.heeblings.api.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "plant_push_messages")
class PlantPushMessage(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val messageId: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore // API 응답 시 user 객체 전체가 노출되지 않도록 설정
    val user: PlantUser,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false)
    val body: String,

    var isRead: Boolean = false,

    val createdAt: LocalDateTime = LocalDateTime.now()
)
