package com.heeee.plant_care_app.domain

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "Plants") // DB 테이블 이름과 매핑
class Plant(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val plantId: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // 외래키
    val user: User,

    var nickname: String?,
    var imageUrl: String?,
    var plantType: String?,

    @Column(nullable = false)
    var startDate: LocalDate,

    @Column(nullable = false)
    var lastWateredDate: LocalDate,

    var lastRepottedDate: LocalDate?,
    var wateringCycleDays: Int?,
    var description: String?,
    var careInfo: String?
)