package kr.heeblings.api.domain

import jakarta.persistence.*

@Entity
@Table(name = "plant_users") // 테이블명을 users -> plant_users로 변경
class PlantUser(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val userId: Long = 0,

    @Column(unique = true, nullable = false)
    val kakaoId: Long,

    var nickname: String?,

    var fcmToken: String?
)
