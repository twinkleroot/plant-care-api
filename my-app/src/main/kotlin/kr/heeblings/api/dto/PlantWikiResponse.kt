package kr.heeblings.api.dto

data class PlantWikiResponse(
    val plantTypeName: String,
    val wateringCycleDays: Int?,
    val description: String?,
    val careInfo: String?
)