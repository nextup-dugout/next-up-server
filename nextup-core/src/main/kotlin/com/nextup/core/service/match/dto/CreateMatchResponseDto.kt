package com.nextup.core.service.match.dto

data class CreateMatchResponseDto(
    val matchRequestId: Long,
    val respondTeamId: Long,
    val message: String?,
)
