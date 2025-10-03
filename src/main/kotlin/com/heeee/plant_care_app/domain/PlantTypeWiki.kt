package com.heeee.plant_care_app.domain

import jakarta.persistence.*

@Entity
@Table(name = "plant_type_wiki")
class PlantTypeWiki(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val plantTypeId: Long = 0,

    @Column(unique = true, nullable = false)
    val plantTypeName: String,

    val wateringCycleDays: Int,
    val description: String,
    val careInfo: String
)