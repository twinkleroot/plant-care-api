package com.heeee.plant_care_app.domain

import jakarta.persistence.*

@Entity
@Table(name = "Users") // DB 테이블 이름과 매핑
class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val userId: Long = 0,

    @Column(unique = true, nullable = false)
    val kakaoId: Long,

    var nickname: String?,

    var fcmToken: String?
)