package kr.heeblings.api.dto

data class SystemConfigResponse(
    val forceUpdate: UpdateConfig?,
    val updateNotice: UpdateConfig?,
    val notice: NoticeConfig?
)

data class UpdateConfig(
    val version: String,
    val startDate: String, // "yyyy-MM-dd"
    val endDate: String    // "yyyy-MM-dd"
)

data class NoticeConfig(
    val title: String,
    val content: String,
    val startDate: String,
    val endDate: String
)